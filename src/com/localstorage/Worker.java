package com.localstorage;

import java.io.*;
import java.sql.Time;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The type Worker.
 */
public class Worker{
    private final File dbFile;

    private boolean newDataAvailable = false;
    /**
     * The Shadow storage.
     */
    ConcurrentHashMap<String, Packet> shadowStorage;
    private final ScheduledExecutorService scheduledExecutorService;
    private boolean shutDownWorker = false;

    /**
     * Instantiates a new Worker.
     *
     * @param dbFile the db file
     */
    Worker(File dbFile)  {
        this.dbFile = dbFile;
        this.shadowStorage = new ConcurrentHashMap<>();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.startScheduler();

    }

    private void startScheduler() {
        this.scheduledExecutorService.scheduleAtFixedRate(createNewThread(), 1, 1, TimeUnit.SECONDS);
    }

    private Thread createNewThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(newDataAvailable) {
                        newDataAvailable = false;
                        writeToFile();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if(shutDownWorker) {
                    scheduledExecutorService.shutdown();
                }
            }
        });
    }

    /**
     * Notify worker.
     *
     * @param shadowStorage the shadow storage
     */
    void notifyWorker(ConcurrentHashMap<String, Packet> shadowStorage) {
        this.shadowStorage = shadowStorage;
        if(!this.newDataAvailable) {
            this.newDataAvailable = true;
        }
    }

/*
    private File createNewFile() throws IOException {
        File dbFile = new File(this.dbPath);
        dbFile.createNewFile();
        return dbFile;
    }
*/


    private synchronized void writeToFile() throws IOException, ClassNotFoundException {

        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(this.dbFile))) {
            os.writeObject(this.shadowStorage);
        }

        /*FileWriter writer = new FileWriter(this.dbFile);
        writer.write(this.shadowStorage.toString());
        writer.flush();
        writer.close();*/
    }

    /**
     * Shut down worker.
     *
     * @param shutDownWorker the shut down worker
     */
    void shutDownWorker(boolean shutDownWorker) {
        this.shutDownWorker = shutDownWorker;
    }
}
