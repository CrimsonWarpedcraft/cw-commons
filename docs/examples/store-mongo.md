---
title: MongoDB Backend
parent: Examples
nav_order: 6
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

Pass a `MongoDbBackend` to [`DataStore.builder`](store.md#advanced-custom-backends-and-serialization)
just like any other backend — the same entry point SQLite uses. The `MongoDbBackend` constructor
throws `IOException` if it can't connect.

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.MongoDbBackend;
import java.io.IOException;

DataStore store;

// In onEnable()
try {
    store = DataStore.builder(new MongoDbBackend("mongodb://localhost:27017", "myDatabase"))
        .name("myplugin")
        .build();
} catch (IOException e) {
    getLogger().severe("MongoDB backend failed: " + e.getMessage());
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

> The builder's default mapper sets field visibility, so plain POJOs and `record`s both serialize
> correctly. Override it with `.mapper(...)` only to add serializers or change Jackson settings.

## Shutting down

The builder's store **owns** the backend, so a single `store.close()` flushes pending writes, stops
the I/O thread, and closes the MongoDB connection:

```java
// In onDisable()
store.close();    // flushes, stops the I/O thread, and closes the MongoClient
```

If you share one `MongoDbBackend` across several stores, build them with `.closeBackend(false)` and
close the backend yourself after closing the stores.

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
DataStore.builder(backend).writePolicy(WritePolicy.WRITE_THROUGH_ATOMIC).build()
```

## Switching from SQLite

Only the backend construction changes; every `repository()`, `get()`, `put()`, `flush()`, and
`close()` call is identical.

```java
// SQLite — the Bukkit builder seeds the file backend and Bukkit serializers
DataStore store = new BukkitDataStoreBuilder("myplugin", getDataFolder()).build();

// MongoDB — the same builder, a different backend
DataStore store = DataStore.builder(new MongoDbBackend(uri, databaseName))
    .name("myplugin")
    .build();
```
