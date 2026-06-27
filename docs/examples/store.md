---
title: Data Store
parent: Examples
nav_order: 3
---

# Data Store

`DataStore` is an **asynchronous, write-behind key-value store**. You read and write Java objects
keyed by anything you can turn into a string; behind the scenes the store serializes them to JSON,
caches them in memory, and flushes them to a backend (SQLite or MongoDB) on a dedicated I/O thread.
Nothing touches disk on the main thread.

This page covers the concepts and the bundled SQLite backend. Once you understand it, wiring up the
[Bukkit helpers](store-bukkit.md) or swapping in [MongoDB](store-mongo.md) is a small step.

## How it's layered

A `DataStore` is a stack of small pieces, each with one job. You normally only touch the top two;
the rest is assembled for you by `getLocalDataStore` (or by hand for [custom
setups](#advanced-custom-backends-and-serialization)).

```text
 Your plugin
     │  store.repository("kills", Integer.class, KeySerializers.forUuid())
     ▼
 DataStore  (ConcurrentDataStore)
     │     • one Repository per namespace; caches and reuses them
     │     • flush() / close() fan out to every repository
     │  get / put / delete / flush  →  CompletableFuture<…>
     ▼
 Repository<K, V>  (one per namespace)
     │     • serializes K via KeySerializer, V via Jackson
     │     • dispatches every operation onto the single I/O thread
     │  String key, String json
     ▼
 CachingBackend  (in-memory cache + write buffer)
     │     • reads served from cache when possible
     │     • writes buffered until flush(), per WritePolicy
     │  cache misses, flushed writes, deletes
     ▼
 StorageBackend
     SqliteBackend (bundled)   │   MongoDbBackend (optional)
```

| Layer | Type | Role |
|-------|------|------|
| Store | `DataStore` → `ConcurrentDataStore` | Owns the namespaces and the I/O thread; the only thing you keep a reference to. |
| Repository | `Repository<K, V>` | A typed view over one namespace. Serializes keys/values and hands work to the I/O thread. |
| Cache | `CachingBackend` | Keeps loaded/written values in memory and buffers writes — see [Caching & write policies](#caching-and-write-policies). |
| Backend | `StorageBackend` → `SqliteBackend` / `MongoDbBackend` | The actual persistence: a SQLite file or a MongoDB database. |

## Quick start

`DataStore.getLocalDataStore(name, dataDir)` builds the whole stack above with a SQLite backend and
sensible defaults — it's the entry point for the common case.

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;

// In onEnable()
DataStore store = DataStore.getLocalDataStore("myplugin", getDataFolder());
```

The `name` is used for two things: the database filename (`myplugin.db` inside `getDataFolder()`)
and the I/O thread name (`myplugin-store-io`, which shows up in thread dumps). The file and any
missing parent directories are created automatically. It throws `IOException` if the database can't
be opened, so handle that the same way you would a bad [config load](config-loading.md#load-in-onenable).

## Namespaced repositories

You never read or write through the `DataStore` directly — you ask it for a `Repository`, which is a
typed, isolated view over one **namespace** (a SQLite table / Mongo collection). Use one namespace
per kind of data.

```java
import com.crimsonwarpedcraft.cwcommons.store.KeySerializers;
import com.crimsonwarpedcraft.cwcommons.store.Repository;
import java.util.UUID;

// Keyed by UUID, storing Integer values
Repository<UUID, Integer> killCounts =
    store.repository("kills", Integer.class, KeySerializers.forUuid());

// Keyed by String, storing String values
Repository<String, String> messages =
    store.repository("motd", String.class, KeySerializers.forString());
```

Asking for the same namespace twice returns the **same** `Repository` instance, so it's cheap to
call `store.repository(...)` wherever you need it rather than passing the handle around. Don't reuse
one namespace with two different value types — the stored JSON won't deserialize back.

The value type can be any Jackson-serializable class: a `record`, a POJO, a boxed primitive, a
`List`, etc. (Bukkit types like `Location` need a [custom serializer](store-bukkit.md#storing-bukkit-types).)

## Reading and writing

Every operation returns a `CompletableFuture`. **The future completes on the store's I/O thread —
not the Bukkit main thread** — so any continuation that calls the Bukkit API must hop back onto the
main thread with the scheduler.

```java
UUID playerId = player.getUniqueId();

// Write — buffered in memory, persisted on the next flush() or close()
killCounts.put(playerId, 42);

// Read — runs on the I/O thread; reschedule to the main thread to touch Bukkit
killCounts.get(playerId).thenAccept(maybeKills -> {
    int kills = maybeKills.orElse(0);
    getServer().getScheduler().runTask(this,
        () -> player.sendMessage("Kills: " + kills));
});

// Delete
killCounts.delete(playerId);
```

`get` returns `CompletableFuture<Optional<V>>` — the `Optional` is empty when the key has never been
stored. `getAll()` returns every entry in the namespace as a `Map<K, V>`.

> **Avoid blocking the main thread.** You *can* call `.get()` on a future to wait for the result,
> but it blocks the calling thread (and throws checked `InterruptedException` / `ExecutionException`).
> Never do this on the Bukkit main thread — it stalls the whole server. Prefer `thenAccept` /
> `thenApply` as above, or do the blocking read inside your own async task.

Failures (e.g. a backend I/O error) surface as an **exceptional completion** wrapping the underlying
`IOException`; attach `.exceptionally(...)` if you need to handle them.

## Caching and write policies

The `CachingBackend` is what makes the store "write-behind", and it's worth understanding because it
governs *when* your data actually hits disk.

- **Reads are cached.** The first `get` for a key loads it from the backend and keeps it in memory;
  later reads of that key (and any key you've written) are served from the cache without disk I/O.
- **Writes are buffered.** Under the default policy, `put` only updates the in-memory cache and marks
  the entry *dirty*. The value is written to the backend the next time something calls `flush()` —
  see [Flushing & lifecycle](#flushing-and-lifecycle). Batching many writes into one flush is what
  keeps the store fast.
- **Deletes are immediate.** `delete` always removes the key from both the cache and the backend
  right away, regardless of policy — it is never buffered.
- **The cache is unbounded.** It grows as keys are loaded and written and lives for the store's
  lifetime; there is no eviction. For per-player data this is fine (it's bounded by your player
  count), but don't use a single store as a cache for millions of keys.

Each store is built with one `WritePolicy`:

| Policy | Behaviour | Use when |
|--------|-----------|----------|
| `CACHE_AND_FLUSH` *(default)* | Buffers writes in memory; persists them on `flush()` or `close()`. | Single-server setups — the fast, normal case. |
| `WRITE_THROUGH_ATOMIC` | Persists every `put` immediately *and* caches it. `flush()` becomes a no-op. | Data must survive a crash without waiting for a flush, or a [shared database is read by multiple nodes](store-mongo.md). |

`getLocalDataStore` always uses `CACHE_AND_FLUSH`. To choose `WRITE_THROUGH_ATOMIC`, construct the
store [by hand](#advanced-custom-backends-and-serialization).

## Flushing and lifecycle

Because writes are buffered, you are responsible for flushing them — otherwise buffered data is lost
if the server stops without a clean shutdown.

```java
// Persist all pending writes across every repository (non-blocking)
store.flush();

// In onDisable() — flushes everything, then releases the backend / I/O thread
store.close();
```

- `store.flush()` fans out to every repository and returns a future that completes when all dirty
  entries have been persisted.
- `store.close()` flushes first, then shuts down the I/O thread and (for a `getLocalDataStore`
  store) closes the SQLite connection. **Always call it in `onDisable()`.**

Three flush triggers cover most plugins; combine them:

1. **On shutdown** — `store.close()` in `onDisable()`. *(Required.)*
2. **When a player leaves** — [`PlayerDataManager`](store-bukkit.md#playerdatamanager) flushes on
   `PlayerQuitEvent`.
3. **On a timer** — [`AutoFlushTask`](store-bukkit.md#periodic-flushing-with-autoflushtask) flushes
   periodically so a crash loses at most one interval of writes.

## Key serialization

A `KeySerializer<K>` turns your key type into the string the backend stores under (and back again).
`KeySerializers` provides the two common ones:

```java
KeySerializers.forUuid();   // KeySerializer<UUID>
KeySerializers.forString(); // KeySerializer<String>
```

For any other key type, implement `KeySerializer<K>`. Note it has **two** methods — `serialize` and
`deserialize` — so it is *not* a functional interface and cannot be written as a lambda:

```java
import com.crimsonwarpedcraft.cwcommons.store.KeySerializer;

KeySerializer<Long> longKeys = new KeySerializer<>() {
    @Override
    public String serialize(Long key) {
        return Long.toString(key);
    }

    @Override
    public Long deserialize(String raw) {
        return Long.parseLong(raw);
    }
};

Repository<Long, String> repo = store.repository("ids", String.class, longKeys);
```

`serialize` is used for every `get`/`put`/`delete`; `deserialize` is only used to reconstruct keys
from `getAll()`, so both directions must round-trip.

## Advanced: custom backends and serialization

`getLocalDataStore` is a convenience wrapper. To control the backend, the write policy, the executor,
or the Jackson `ObjectMapper` (needed for [Bukkit types](store-bukkit.md#storing-bukkit-types)),
assemble the stack yourself. The pieces map one-to-one onto the [layer diagram](#how-its-layered):

```java
import com.crimsonwarpedcraft.cwcommons.store.CachingBackend;
import com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStore;
import com.crimsonwarpedcraft.cwcommons.store.SqliteBackend;
import com.crimsonwarpedcraft.cwcommons.store.ThreadedRepositoryBuilder;
import com.crimsonwarpedcraft.cwcommons.store.WritePolicy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.concurrent.Executors;

// Reproduce getLocalDataStore's mapper config, then customize as needed.
// Without setVisibility, POJOs whose fields are private (and have no getters)
// serialize to "{}" — this line is what makes plain data classes work.
ObjectMapper mapper = new ObjectMapper()
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

SqliteBackend backend = new SqliteBackend(new File(getDataFolder(), "myplugin.db"));
DataStore store = new ConcurrentDataStore(
    new ThreadedRepositoryBuilder(
        new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
        Executors.newSingleThreadExecutor(r -> new Thread(r, "myplugin-store-io")),
        mapper));
```

> **Ownership note.** The public 3-arg `ThreadedRepositoryBuilder` constructor does **not** take
> ownership of the backend — `store.close()` shuts down the executor but does *not* close the
> `SqliteBackend` or its file handle. Either close the backend yourself in `onDisable()`, or use
> `getLocalDataStore`, which manages the backend lifecycle for you.

From here you can:

- swap `SqliteBackend` for [`MongoDbBackend`](store-mongo.md);
- choose `WritePolicy.WRITE_THROUGH_ATOMIC`;
- register [`Location` / `ItemStack` serializers](store-bukkit.md#storing-bukkit-types) on the mapper.
