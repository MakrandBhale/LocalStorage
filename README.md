# LocalStorage
a key-value based datastore

### How to use
Just copy the `LocalStorage.jar` file in the `/lib` folder of your project.
Which you can find [here](https://github.com/MakrandBhale/LocalStorage/blob/master/out/artifacts/LocalStorage_jar/LocalStorage.jar).
  
### Syntax

```java
LocalStorage localStorage = new LocalStorage.Builder()
                                    .atPath(YOUR_PATH)
                                    .withFileName("DB_FILE_NAME.db")
                                    .build();

// Example usage
localStorage.create("sampleKey", "{someKey: someVal}");

```

Also supports `read` and `delete` operations. 

### Supported Methods
 For creating new entry in the database.
```java
create(String key, String jsonObject);
```

```java
create(String key, JsonObject jsonObject);
```
You can also specify a time to live parameter.
```java
create(String key, String jsonObject, int ttl);
```
```java
create(String key, JsonObject jsonObject, int ttl);
```
`ttl` specifies "time to live" for a specific key. After the time limit is exceeded the key is no longer valid.

For reading the previously stored value. Returns an instance of JsonObject.
```java
read(String key);
```
For deleting a value along with its key from database.
```java
delete(String key);
```

Always remember to close the database connection by calling following method. If you don't, then the background worker will not stop, even after your main programs completes its execution.
```java
close();
```

## How does it work

It uses a bridge architecture. When a new instance of `LocalStorage` is created, it spawns a worker thread and creates
a buffer which I like to call as a `ShadowStorage`. <br> It's a ConcurrentHashMap, all read, write, delete operations
are performed on this `ShadowStorage` and which are then conveniently written to a database file by the background worker.
<br><br>
What is the advantage of this approach?<br>
Ans. Faster read, write operations. Fun fact: The library takes ~500 milliseconds to write 10000 records.
<br><br>
As the `ShadowStorage` uses ConcurrentHashMap it is thread-safe and the worker thread locks database file thus making it inaccessible
by any other process.


<br>
Note: This project was created as a submission for Freshworks Assignment. 
