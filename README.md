# LocalStorage
a key-value based datastore

### How to use is 
Just copy the `LocalStorage.jar` file in the `/lib` folder of your project.
Note: the jar file is not updated, so you might want to build the artifact from the project source. 
### Syntax

```
LocalStorage localStorage = new LocalStorage.Builder()
.atPath(YOUR_PATH)
.fileName("DB_FILE_NAME.db")
.build();

// Example usage
localStorage.create("sampleKey", "{someKey: someVal}");

```

Also supports `read` and `delete` operations. 

