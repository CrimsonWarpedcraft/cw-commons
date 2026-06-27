---
title: MongoDB Backend
parent: Examples
nav_order: 5
---

# MongoDB Backend

`MongoDbBackend` is a drop-in alternative to `SqliteBackend`. Everything you learned in the
[Data Store](store.md) guide — repositories, the [caching layer](store.md#caching-and-write-policies),
[async reads/writes](store.md#reading-and-writing), [flushing](store.md#flushing-and-lifecycle),
[key serialization](store.md#key-serialization) — applies unchanged. **Only the bottom layer of the
[stack](store.md#how-its-layered) changes.**

Each repository namespace becomes a MongoDB collection; each entry is stored as a document of the
form `{ "_id": <key>, "value": <json> }`.

## Build dependency

The MongoDB driver is **not** bundled — add it to your own plugin:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.mongodb:mongodb-driver-sync:5.8.0")
}
```

## Setup

There is no `getLocalDataStore` equivalent for MongoDB, so you assemble the
[stack](store.md#how-its-layered) by hand — the same construction shown in
[Advanced: custom backends](store.md#advanced-custom-backends-and-serialization), with
`MongoDbBackend` in place of `SqliteBackend`. The constructor throws `IOException` if it can't
connect.

```java
import com.crimsonwarpedcraft.cwcommons.store.CachingBackend;
import com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStore;
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.MongoDbBackend;
import com.crimsonwarpedcraft.cwcommons.store.ThreadedRepositoryBuilder;
import com.crimsonwarpedcraft.cwcommons.store.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.Executors;

// Keep references to both the backend and the store — you need both at shutdown (see below).
MongoDbBackend backend;
DataStore store;

// In onEnable()
try {
    backend = new MongoDbBackend("mongodb://localhost:27017", "myDatabase");
    store = new ConcurrentDataStore(
        new ThreadedRepositoryBuilder(
            new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
            Executors.newSingleThreadExecutor(r -> new Thread(r, "myplugin-store-io")),
            new ObjectMapper()));
} catch (IOException e) {
    getLogger().severe("MongoDB backend failed: " + e.getMessage());
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

> Using a plain `new ObjectMapper()` works for `record`s (Jackson binds them via the canonical
> constructor). For POJOs with private fields, configure field visibility as shown in the
> [custom-mapper example](store.md#advanced-custom-backends-and-serialization), otherwise they
> serialize to `{}`.

## Shutting down

This is the one place manual construction differs from `getLocalDataStore`: **the store does not own
the backend, so closing the store does not close the MongoDB connection.** Close the backend
yourself, after the store:

```java
// In onDisable()
store.close();    // flushes pending writes and stops the I/O thread
backend.close();  // closes the underlying MongoClient
```

(`getLocalDataStore` owns its `SqliteBackend` and closes it for you — that convenience doesn't exist
for the hand-assembled MongoDB store.)

## Choosing a write policy on a shared database

The [write policy](store.md#caching-and-write-policies) matters more with MongoDB, because a Mongo
database is often shared by several servers (a proxy network, for example):

- **One server owns the data** → keep the default `CACHE_AND_FLUSH` for the same batching speed as
  SQLite.
- **Multiple servers read/write the same collections** → use `WRITE_THROUGH_ATOMIC` so each write
  lands in MongoDB immediately and other nodes see it without waiting for a flush cycle. Note the
  in-memory cache still serves local reads, so a node won't observe another node's update until it
  loads that key fresh.

```java
new CachingBackend(backend, WritePolicy.WRITE_THROUGH_ATOMIC)
```

## Switching from SQLite

Only the backend construction changes; every `repository()`, `get()`, `put()`, `flush()`, and
`close()` call is identical.

```java
// SQLite — the factory manages the executor and backend lifecycle for you
DataStore store = BukkitDataStores.getLocalDataStore("myplugin", getDataFolder());

// MongoDB — you own the executor and the backend lifecycle
MongoDbBackend backend = new MongoDbBackend(uri, databaseName);
DataStore store = new ConcurrentDataStore(
    new ThreadedRepositoryBuilder(
        new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
        Executors.newSingleThreadExecutor(),
        new ObjectMapper()));
```
