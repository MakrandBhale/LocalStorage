package Tests;

import com.localstorage.LocalStorage;
import com.sun.prism.shader.AlphaOne_Color_AlphaTest_Loader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageTest {

    @org.junit.jupiter.api.Test
    void create() throws Exception {
        LocalStorage localStorage = new LocalStorage.Builder().build();
        String string = null;
        try {
            localStorage.create(null, (String) null, -1);
            fail("Method should have thrown an  exception");
        } catch (Exception e) {
            assert e.getClass() == IllegalArgumentException.class;
        }
    }

    @org.junit.jupiter.api.Test
    void reallyLongKeyTest() {
        String longString = "123456789451254875421548754215487521247541245751245752145464564564564564564564545645645656564548744845457845485612345645";
        Exception exception = assertThrows(Exception.class, () -> {
            LocalStorage localStorage = new LocalStorage.Builder().build();
            localStorage.create(longString, "{hello: world}");
        });

        String expectedMessage = "Key size must be greater than 0 and less than or equal to 32. Provided key length: " + longString.length();
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @org.junit.jupiter.api.Test
    void existingKeyTest() {
        String key = "test123";
        Exception exception = assertThrows(Exception.class, () -> {
            LocalStorage localStorage = new LocalStorage.Builder().build();
            localStorage.create(key, "{hello: world}");
            localStorage.create(key, "{world: hello}");

        });

        String expectedMessage = "Key already exists";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @org.junit.jupiter.api.Test
    void timeToLiveTest() {
        String key = "test123";
        Exception exception = assertThrows(Exception.class, () -> {
            LocalStorage localStorage = new LocalStorage.Builder().build();
            localStorage.create(key, "{hello: world}", 1);
            try
            {
                System.out.println("Waiting");
                Thread.sleep(2000);
            }
            catch(InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            localStorage.create(key, "{world: hello}");
        });

        String expectedMessage = "Key exceeded its time to live limit, will be deleted.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}