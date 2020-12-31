package com.localstorage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalStorage{
    private ConcurrentHashMap<String, String> shadowStorage;
    private final Worker worker;
    private static final String FILE_NAME = "datastore.db";
    private final String dbFilePath;
    public LocalStorage(String path) throws IOException {
        this.dbFilePath = path.concat("/").concat(FILE_NAME);
        try {
            this.shadowStorage = init();
        } catch (Exception e) {
            System.out.println("Database not found. New database will be created at given path.");
            this.shadowStorage = new ConcurrentHashMap<>();
        }
        worker = new Worker(dbFilePath);
    }

    public LocalStorage() throws IOException {
        this(".");
    }

    public void create(String key, String jsonString) throws Exception{
        if(!validate(key, jsonString)) return;
        if(shadowStorage.containsKey(key)) {
            throw new Exception("Key already exists");
        }

        shadowStorage.put(key, jsonString);
        worker.notifyWorker(this.shadowStorage);
    }

    private boolean validate(String key, String jsonString) throws Exception {
        if(key.length() == 0 || key.length() > 32) {
            throw new Exception("Key size must be greater than 0 and less than or equal to 32. Provided key length: " + key.length());
        }

        return isValidJson(jsonString);
    }


    private boolean isValidJson(String jsonString) {
        /*Check id string is a valid json object or not*/
        Gson gson = new Gson();
        try {
            gson.fromJson(jsonString, JsonObject.class);
            return true;
        } catch (JsonParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    /***
     * retrieves the value associated with given key.
     * @param key a String to retrieve associated jsonObject
     * @return returns JsonObject
     * @throws Exception th
     */
    public JsonObject read(String key) throws Exception {
        if(this.shadowStorage.size() == 0) {
            throw new Exception("Provided key not found in database. Available records: 0");
        }

        String jsonString = this.shadowStorage.getOrDefault(key, null);
        if(jsonString == null) {
            throw new Exception("Provided key not found in database. Available records: " + this.shadowStorage.size());
        }
        return new Gson().fromJson(jsonString, JsonObject.class);
    }

    public void delete(String key) throws Exception{
        if(this.shadowStorage.size() == 0) {
            throw new Exception("Provided key not found in database. Available records: 0");
        }
        this.shadowStorage.remove(key);
        worker.notifyWorker(this.shadowStorage);
    }

    public ConcurrentHashMap<String, String> init() throws ClassNotFoundException, FileNotFoundException, IOException, EOFException {
        Object ii = new ObjectInputStream(new FileInputStream(this.dbFilePath)).readObject();
        return (ConcurrentHashMap<String, String>) ii;
    }


    public void close() {
        this.worker.shutDownWorker(true);
    }
}
