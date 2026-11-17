---
title: Data Store
parent: Examples
nav_order: 3
---

# Data Store

`DataStore` is an **asynchronous, write-behind key-value store**. You read and write Java objects
keyed by anything you can turn into a string; behind the scenes the store serializes them to JSON,
caches them in memory, and flushes them to a backend (SQLite or MongoDB) in one serial I/O lane.
By default that lane uses a dedicated daemon thread, keeping repository I/O off the main thread.
A custom direct executor such as `Runnable::run` instead runs I/O on its caller.

SQLite construction opens the file and initializes its schema synchronously on the calling thread.

This page covers the concepts and the bundled SQLite backend. Once you understand it, wiring up the
[Bukkit helpers](store-bukkit.md) or swapping in [MongoDB](store-mongo.md) is a small step.

## How it's layered

A `DataStore` is a stack of small pieces, each with one job. You normally only touch the top two;
the rest is assembled for you by `DataStore.builder(...)` (or `BukkitDataStoreBuilder` for the Bukkit
common case — see [custom setups](#advanced-custom-backends-and-serialization)).

```text
 Your plugin
     │  store.repository("kills", Integer.class, KeySerializers.forUuid())
     ▼
 DataStore  (ConcurrentDataStore)
     │     • one Repository per namespace; caches and reuses them
     │     • flush() / close() flush every registered namespace
     │  get / put / delete / flush  →  CompletableFuture<…>
     ▼
 Repository<K, V>  (one per namespace)
     │     • serializes K via KeySerializer, V via Jackson
     │     • dispatches every operation onto one logical I/O lane
     │  String key, String json
     ▼
 CachingBackend  (in-memory cache + write buffer — skipped when CacheMode.NONE)
     │     • reads served from cache when possible
     │     • writes buffered until flush(), per the selected CacheMode
     │  cache misses, flushed writes, deletes
     ▼
 StorageBackend
     SqliteBackend (bundled)   │   MongoDbBackend (optional)
```

| Layer | Type | Role |
|-------|------|------|
| Store | `DataStore` → `ConcurrentDataStore` | Owns the namespaces and default I/O thread; manages the shared serial lane. |
| Repository | `Repository<K, V>` | A typed view over one namespace. Serializes keys/values and hands work to the I/O thread. |
| Cache | `CachingBackend` | Keeps loaded/written values in memory and buffers writes — see [Caching & write policies](#caching-and-write-policies). |
| Backend | `StorageBackend` → `SqliteBackend` / `MongoDbBackend` | The actual persistence: a SQLite file or a MongoDB database. |

Backend construction owns any connection or client it acquires: failed initialization closes it,
preserving the original failure and suppressing cleanup failures. Successful construction keeps it
open until the backend is closed. SQLite-JDBC is bundled without relocation; MongoDB's driver is
optional and unbundled, and [MongoDB construction is lazy](store-mongo.md#setup), not a connectivity
or authentication check.

## Quick start

For a Bukkit plugin — the common case —
[`BukkitDataStoreBuilder`](store-bukkit.md#the-easy-way-bukkitdatastorebuilder)
builds the whole stack above with a SQLite backend, sensible defaults, and the Bukkit
(de)serializers already registered:

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.BukkitDataStoreBuilder;

// In onEnable()
DataStore store = new BukkitDataStoreBuilder("myplugin", getDataFolder()).build();
```

Not a Bukkit plugin, or need a custom backend or serializers? Assemble the store yourself (see
[Advanced](#advanced-custom-backends-and-serialization)).

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

Every operation returns a `CompletableFuture`. **Callback thread affinity is not guaranteed.**
Non-async continuations can run on the thread completing the future or on the caller attaching
them, and direct executors run I/O on their caller. Any continuation that calls the Bukkit API
must explicitly hop back onto the main thread with the scheduler.

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
governs *when* your data actually hits disk. You pick its behaviour with a `CacheMode` on the
builder.

- **Reads are cached** *(the two cached modes)*. The first `get` for a key loads it from the backend
  and keeps it in memory; later reads of that key (and any key you've written) are served from the
  cache without disk I/O.
- **Writes are buffered** *(`CACHE_AND_FLUSH`)*. `put` only updates the in-memory cache and marks
  the entry *dirty*. The value is written to the backend the next time something calls `flush()` —
  see [Flushing & lifecycle](#flushing-and-lifecycle). Batching many writes into one flush is what
  keeps the store fast.
- **Deletes go straight to the backend.** Regardless of cache mode, `delete` calls the backend
  first and removes the cached entry only after that call succeeds. A failed delete preserves the
  previous cached value, including any dirty write waiting to be flushed.
- **The cache is unbounded.** It grows as keys are loaded and written and lives for the store's
  lifetime; there is no eviction. For per-player data this is fine (it's bounded by your player
  count), but don't use a single store as a cache for millions of keys.

Each store is built with one `CacheMode`:

| Mode | Behaviour | Use when |
|------|-----------|----------|
| `CACHE_AND_FLUSH` *(default)* | Caches reads; buffers writes in memory and persists them on `flush()` or `close()`. | Single-server setups — the fast, normal case. |
| `WRITE_THROUGH_ATOMIC` | Calls the backend for every `put`, then updates the cache on success. `flush()` still flushes the backend. | One server owns the data and the backend persists each save without waiting for a flush. |
| `NONE` | No cache at all: every read hits the backend fresh and every write goes straight through. Flushes are delegated to the backend. | A [database is shared by multiple servers](store-mongo.md) — each node must see the others' writes. |

The builder defaults to `CACHE_AND_FLUSH`. To choose another mode, pass it to `.cacheMode(...)`
(see [Advanced](#advanced-custom-backends-and-serialization)).

Failed write-through saves leave the previous cache state unchanged. Buffered flushes save dirty
entries and then flush the backend, even when nothing is cached locally. Entries become clean only
after **all** saves and the backend flush succeed. After a failure, dirty values remain available
for retry; already successful saves may be repeated as idempotent replacements of the same keys.
There is no multi-key transaction or rollback guarantee: a backend can commit a write and then
throw, so an exception does not establish the backend's commit outcome. Durability and atomicity
depend on the backend, and a custom buffering backend still needs flushing in write-through mode.
These cache guarantees assume serialized operations on the decorator and its delegate (for example,
through one store I/O executor); concurrent maps alone do not coordinate independent callers.

> **Why `NONE` for a shared database?** The cache is per-process — a `WRITE_THROUGH_ATOMIC` write
> lands in the shared database immediately, but each node keeps serving its *own* cached copy of a
> key until it reloads it, so nodes drift out of sync. `NONE` removes the cache, so every read
> reflects the latest state in the shared backend.

## Flushing and lifecycle

Because writes are buffered, you are responsible for flushing them — otherwise buffered data is lost
if the server stops without a clean shutdown.

```java
// Persist all pending writes across every repository (non-blocking)
store.flush();

// In onDisable() — flushes everything, then releases the backend / I/O thread
store.close();
```

- `store.flush()` flushes every registered namespace in the shared lane and returns a future that
  completes when all dirty entries have been persisted.
- `store.close()` atomically stops admission: new namespace lookups (including existing namespaces)
  throw `RejectedExecutionException`; operations on existing repository handles and `store.flush()`
  return exceptionally completed futures. Already admitted operations drain in order, then every
  registered namespace is flushed, the owned backend is closed, and the owned default executor is
  shut down and awaited. Caller-supplied executors are **never** shut down; `.closeBackend(false)`
  retains the backend but still drains and flushes. **Always close in `onDisable()`.**
- Close is idempotent and concurrency-safe. Earlier operation failures remain the primary close
  failure; later flush/cleanup failures are suppressed. Cleanup still attempts all namespaces,
  backend release, and owned executor shutdown after a failure.
- `.closeTimeout(Duration.ofSeconds(30))` configures the default 30-second wait per close call,
  including owned worker termination (`Duration.ZERO` checks without waiting). Timeout or
  interruption throws `IOException` reporting incomplete persistence/resource release; interruption
  preserves interrupt status. No queued writes are cancelled, no `shutdownNow()` is used, and the
  backend is not closed under active work. Unfinished terminal cleanup remains queued, and a later
  close can wait again. **Stuck backend I/O cannot be forcibly guaranteed to finish.**
- Call blocking close from the lifecycle owner, not from a store operation or an inline callback
  executing inside it: this fails clearly rather than deadlocking. The timeout bounds waiting,
  not synchronous execution by a direct executor. Executor dispatch rejection fails affected
  request futures and is reported by close; terminal cleanup is safe to attempt on the rejecting
  caller once no I/O is active, and that synchronous cleanup cannot be preempted by the wait limit.
- One lane is shared across namespaces, even with a multi-worker executor. Cancelling a returned
  future does not cancel its admitted I/O; dispatch rejection is an explicit exceptional completion,
  not silent loss of a queued write.

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

[`BukkitDataStoreBuilder`](store-bukkit.md#the-easy-way-bukkitdatastorebuilder) covers the
common case. For a custom backend, cache mode, executor, or `ObjectMapper` (your own serializers),
use `DataStore.builder(backend)` and set only the knobs you care about — everything else falls back
to the same defaults:

```java
import com.crimsonwarpedcraft.cwcommons.store.CacheMode;
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.SqliteBackend;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

// The builder's default mapper already sets field visibility so plain data classes
// (private fields, no getters) serialize correctly. Build your own only to add
// serializers or change Jackson settings.
ObjectMapper mapper = new ObjectMapper()
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

DataStore store = DataStore.builder(new SqliteBackend(new File(getDataFolder(), "myplugin.db")))
    .name("myplugin")                          // I/O thread name: myplugin-store-io
    .cacheMode(CacheMode.CACHE_AND_FLUSH)
    .mapper(mapper)
    .build();
```

> **Ownership note.** By default the store **owns** the backend — `store.close()` closes the
> `SqliteBackend` (and its file handle) for you. Call `.closeBackend(false)` when the backend is
> shared elsewhere and you want to close it yourself.

From here you can:

- swap `SqliteBackend` for [`MongoDbBackend`](store-mongo.md);
- choose `CacheMode.WRITE_THROUGH_ATOMIC`, or `CacheMode.NONE` to disable caching for a shared
  database;
- register [`Location` / `ItemStack` serializers](store-bukkit.md#storing-bukkit-types) on the mapper.
