package com.localstorage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javafx.concurrent.WorkerStateEvent;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Local storage.
 */
public class LocalStorage{
    private ConcurrentHashMap<String, Packet> shadowStorage;
    private Worker worker;
    private final File dbFile;
    private static final String DEFAULT_FILE_NAME = "datastore.db";
    private static final String DEFAULT_PATH = ".";

    /***
     *  Builder class for LocalStorage.
     */
    public static class Builder {
        private String path = DEFAULT_PATH;
        private String fileName = DEFAULT_FILE_NAME;

        /***
         * Sets custom path to store the LocalStorage File.
         * @param path custom path to store the file.
         * @return instance of the Builder class
         */
        public LocalStorage.Builder atPath(String path) {
            this.path = path;
            return this;
        }

        /***
         * Sets a file name for database file. Default: datastore.db
         * @param fileName defines a filename.
         * @return returns instance of the Builder class.
         */
        public LocalStorage.Builder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /***
         * Creates an instance of LocalStorage.
         * @return returns instance of LocalStorage.
         * @throws Exception the exception
         */
        public LocalStorage build() throws Exception {
            this.path = this.path.concat("/").concat(this.fileName);
            File dbFile = new File(this.path);
            dbFile.createNewFile();
            LocalStorage localStorage = new LocalStorage(dbFile);

            localStorage.worker = localStorage.createNewWorker(dbFile);
            return localStorage;
        }
    }



    private LocalStorage(File dbFile) {
        this.dbFile = dbFile;
        try {
            this.shadowStorage = init();
        } catch (FileNotFoundException e) {
            System.out.println("Database file not found. New one will be created at given path.");
        } catch (EOFException e) {
            System.out.println("Database is empty. Initialising shadow storage with 0 records");
            this.shadowStorage = new ConcurrentHashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private Worker createNewWorker(File dbFile) {
        return new Worker(dbFile);
    }


    /***
     * a bare bones create method.
     * @param key specifying the key of the value
     * @param jsonString jsonObject in string format
     * @param ttl the ttl
     * @throws Exception about the validity  of provided key, value.
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
     * @throws Exception the exception
     */
    public void create(String key, JsonObject jsonObject) throws Exception{
        String jsonString = jsonObject.toString();
        create(key, jsonString, -1);
    }

    /***
     *
     * @param key specifying the key of the value
     * @param jsonObject json object.
     * @param ttl time to live for the key.
     * @throws Exception the exception
     */
    public void create(String key, JsonObject jsonObject, int ttl) throws Exception {
        String jsonString = jsonObject.toString();
        create(key, jsonString, ttl);
    }

    /***
     *
     * @param key specifying the key of the value
     * @param jsonString jsonObject in string format.
     * @throws Exception the exception
     */
    public void create(String key, String jsonString) throws Exception {
        create(key, jsonString, -1);
    }



    private boolean validate(String key, String jsonString) throws Exception {
        if(key == null || jsonString == null) throw new IllegalArgumentException("Key and object can not be null.");
        if(key.length() == 0 || key.length() > 32) {
            throw new Exception("Key size must be greater than 0 and less than or equal to 32. Provided key length: " + key.length());
        }

        return isValidJson(jsonString);
    }


    /***
     * checks validity and syntax of provided json string.
     * @param jsonString json object in string format.
     * @return true if the string is a valid Json object.
     */
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

    /**
     * Delete.
     *
     * @param key the key
     * @throws Exception the exception
     */
    public void delete(String key) throws Exception{
        if(this.shadowStorage.size() == 0) {
            throw new Exception("Provided key not found in database. Available records: 0");
        }
        this.shadowStorage.remove(key);
        worker.notifyWorker(this.shadowStorage);
    }

    /***
     * initialising the shadow storage with the data stored in the database file.
     * @return instance of ConcurrentHashMap
     * @throws ClassNotFoundException throws when the objects stored in db file are not of type Packet.
     * @throws FileNotFoundException throws when database file is not found.
     * @throws IOException throws when IOException occurs, possibly db file access is denied.
     * @throws EOFException throws when the db file is empty.
     */
    @SuppressWarnings("unchecked")
    public ConcurrentHashMap<String, Packet> init() throws ClassNotFoundException, FileNotFoundException, IOException, EOFException {
        Object ii = new ObjectInputStream(new FileInputStream(this.dbFile)).readObject();
        return (ConcurrentHashMap<String, Packet>) ii;
    }


    /***
     * closes db connection.
     * Shuts down the background worker.
     */
    public void close()  {
        this.worker.shutDownWorker(true);

    }

    /***
     * Checks validity of the key
     * @param packet instance of packet class.
     * @return returns the if the given data is expired or not.
     */
    private boolean isExpired(Packet packet) {
        return packet.getValidTill() > 0 && packet.getValidTill() < System.currentTimeMillis();
    }



}
