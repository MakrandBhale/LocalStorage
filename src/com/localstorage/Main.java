package com.localstorage;

public class Main {

    public static void main(String[] args) throws Exception {
        LocalStorage localStorage = new LocalStorage();
        //localStorage.create("test1", "{ttl: 5}", 10);
        System.out.println(localStorage.read("test1"));
        localStorage.close();
    }
}
