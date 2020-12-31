package com.localstorage;

import java.io.Serializable;

/***
 *  a class incorporating with timeToLive and JsonObject.
 */
public class Packet implements Serializable {
    private long validTill;
    private String jsonString;

    /***
     *
     * @param ttl time to persist the key in database (time in seconds)
     * @param jsonString a json object in String format.
     */
    Packet(int ttl, String jsonString) {
        if(ttl >= 0) {
            // converting seconds to milliseconds and setting an expiration date
            this.validTill = System.currentTimeMillis() + ttl * 1000;
        } else {
            this.validTill = -1;
        }
        this.jsonString = jsonString;
    }
    /***
     *
     * @param jsonString a json object in string format
     */
    Packet(String jsonString) {
        this(-1, jsonString);
    }


    public long getValidTill() {
        return validTill;
    }

    public void setValidTill(long validTill) {
        this.validTill = validTill;
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }
}
