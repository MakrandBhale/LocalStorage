package com.localstorage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalStorage{
    private ConcurrentHashMap<String, Packet> shadowStorage;
    private final Worker worker;
    private static final String FILE_NAME = "datastore.db";
    private final String dbFilePath;



    public LocalStorage(String path) throws IOException {
        this.dbFilePath = path.concat("/").concat(FILE_NAME);
        try {
            this.shadowStorage = init();
        } catch (Exception e) {
            System.out.println("Database not found or it is empty. New database will be created at given path.");
            this.shadowStorage = new ConcurrentHashMap<>();
        }
        worker = new Worker(dbFilePath);
    }

    public LocalStorage() throws IOException {
        this(".");
    }

    /***
     * a bare bones create method.
     * @param key specifying the key of the value
     * @param jsonString jsonObject in string format
     * @throws Exception
     */

    public void create(String key, String jsonString, int ttl) throws Exception{
        if(!validate(key, jsonString)) return;
        if(shadowStorage.containsKey(key)) {
            Packet packet = shadowStorage.getOrDefault(key, null);
            if(packet == null) {
                throw new Exception("data not found");
            }
            /* if key is expired it can be reused.*/
            if(!isExpired(packet)) {
                throw new Exception("Key already exists");
            }
        }

        Packet newPacket = new Packet(ttl, jsonString);
        shadowStorage.put(key, newPacket);
        worker.notifyWorker(this.shadowStorage);
    }


    /***
     * @param key specifying the key of the value
     * @param jsonObject json object.
     * @throws Exception
     */
    public void create(String key, JsonObject jsonObject) throws Exception{
        String jsonString = jsonObject.toString();
        create(key, jsonString);
    }

    public void create(String key, String jsonString) throws Exception {
        Packet packet = new Packet(jsonString);
        create(key, packet.toString());
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
            throw new Exception( "'" + key + "' key not found in database. Available records: 0");
        }

        Packet packet = this.shadowStorage.getOrDefault(key, null);
        if(packet == null) {
            throw new Exception("Provided key not found in database. Available records: " + this.shadowStorage.size());
        }

        if(isExpired(packet)) {
            this.delete(key); // deleting key and data.
            throw new Exception("Key exceeded its time to live limit, will be deleted.");
        }

        String jsonString = packet.getJsonString();
        return new Gson().fromJson(jsonString, JsonObject.class);
    }

    public void delete(String key) throws Exception{
        if(this.shadowStorage.size() == 0) {
            throw new Exception("Provided key not found in database. Available records: 0");
        }
        this.shadowStorage.remove(key);
        worker.notifyWorker(this.shadowStorage);
    }

    public ConcurrentHashMap<String, Packet> init() throws ClassNotFoundException, FileNotFoundException, IOException, EOFException {
        Object ii = new ObjectInputStream(new FileInputStream(this.dbFilePath)).readObject();
        return (ConcurrentHashMap<String, Packet>) ii;
    }


    public void close() {
        this.worker.shutDownWorker(true);
    }

    /***
     * Checks validity of the key
     * @param packet
     * @return
     */
    private boolean isExpired(Packet packet) {
        return packet.getValidTill() > 0 && packet.getValidTill() < System.currentTimeMillis();
    }
}
