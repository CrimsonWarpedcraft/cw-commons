# MongoDB Data Store

`MongoDbBackend` is a drop-in alternative to `SqliteBackend` that persists data to MongoDB.
The API on the `DataStore` is identical — only the backend construction differs.

The MongoDB driver is **not** bundled. Add it to your own plugin's dependencies.

## Build dependency

```groovy
// build.gradle
dependencies {
    implementation 'org.mongodb:mongodb-driver-sync:5.8.0'
}
```

## Setup

```java
import com.crimsonwarpedcraft.cwcommons.store.CachingBackend;
import com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStore;
import com.crimsonwarpedcraft.cwcommons.store.MongoDbBackend;
import com.crimsonwarpedcraft.cwcommons.store.ThreadedRepositoryBuilder;
import com.crimsonwarpedcraft.cwcommons.store.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.Executors;

// In onEnable()
try {
    MongoDbBackend backend = new MongoDbBackend("mongodb://localhost:27017", "myDatabase");
    DataStore store = new ConcurrentDataStore(
        new ThreadedRepositoryBuilder(
            new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
            Executors.newSingleThreadExecutor(r -> new Thread(r, "myplugin-store-io")),
            new ObjectMapper()));
} catch (IOException e) {
    getLogger().severe("MongoDB backend failed: " + e.getMessage());
    getServer().getPluginManager().disablePlugin(this);
}
```

## Switching from SQLite

Only the backend construction changes. All `repository()`, `flush()`, and `close()` calls
are the same.

```java
// SQLite (via factory — manages executor and lifecycle automatically)
DataStore store = DataStore.getLocalDataStore("myplugin", getDataFolder());

// MongoDB (manual construction — caller owns the executor and backend lifecycle)
MongoDbBackend backend = new MongoDbBackend(uri, databaseName);
DataStore store = new ConcurrentDataStore(
    new ThreadedRepositoryBuilder(
        new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
        Executors.newSingleThreadExecutor(),
        new ObjectMapper()));
```
