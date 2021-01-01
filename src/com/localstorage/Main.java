package com.localstorage;

/**
 * The type Main.
 */
public class Main {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        LocalStorage.Builder builder = new LocalStorage.Builder().withFileName("shitstorm.db");
        LocalStorage localStorage = builder.build();

        localStorage.create("test", "{hello: world}");
        System.out.println(localStorage.read("test"));
        localStorage.close();

    }
}
