# SQLite Data Store

`DataStore` is a write-behind key-value store organized into namespaced `Repository` instances.
`DataStore.getLocalDataStore()` creates a batteries-included SQLite-backed store that owns its
backend and I/O thread.

Writes are buffered in memory and flushed to disk asynchronously on a dedicated daemon thread.
All methods return `CompletableFuture` — call `.get()` when you need the result synchronously,
or chain with `.thenAccept()` / `.thenApply()` for non-blocking code.

## Setup

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;

// In onEnable()
DataStore store = DataStore.getLocalDataStore("myplugin", getDataFolder());
```

`getLocalDataStore` uses `"myplugin"` as both the database filename (`myplugin.db` inside
`getDataFolder()`) and the I/O thread name (`myplugin-store-io`). The file and parent
directories are created automatically.

## Namespaced repositories

Each `Repository` is isolated by a namespace string. Use one namespace per data category.

```java
import com.crimsonwarpedcraft.cwcommons.store.KeySerializers;
import com.crimsonwarpedcraft.cwcommons.store.Repository;
import java.util.UUID;

// Keyed by UUID
Repository<UUID, Integer> killCounts =
    store.repository("kills", Integer.class, KeySerializers.forUuid());

// Keyed by String
Repository<String, String> messages =
    store.repository("motd", String.class, KeySerializers.forString());
```

## Reading and writing

```java
UUID playerId = player.getUniqueId();

// Write (buffered — flushed on the next flush() or close())
killCounts.put(playerId, 42);

// Read (no I/O if value was already loaded or written)
int kills = killCounts.get(playerId).get().orElse(0);

// Delete
killCounts.delete(playerId);
```

## Flushing and closing

```java
// Flush all pending writes to disk (non-blocking)
store.flush();

// In onDisable() — flushes then closes the database connection
store.close();
```

Pair with `AutoFlushTask` for periodic flushing between player quits — see
[`store-auto-flush.md`](store-auto-flush.md).

## Custom key types

Implement `KeySerializer<K>` to use any key type:

```java
import com.crimsonwarpedcraft.cwcommons.store.KeySerializer;

KeySerializer<Long> longSerializer = key -> Long.toString(key);
Repository<Long, String> repo = store.repository("ids", String.class, longSerializer);
```

## Custom ObjectMapper

For custom serialization (e.g. Bukkit `Location` or `ItemStack`), construct the store manually
instead of using `getLocalDataStore`:

```java
import com.crimsonwarpedcraft.cwcommons.store.CachingBackend;
import com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStore;
import com.crimsonwarpedcraft.cwcommons.store.SqliteBackend;
import com.crimsonwarpedcraft.cwcommons.store.ThreadedRepositoryBuilder;
import com.crimsonwarpedcraft.cwcommons.store.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.util.concurrent.Executors;

ObjectMapper mapper = new ObjectMapper().registerModule(new SimpleModule()
    .addSerializer(Location.class, new LocationSerializer())
    .addDeserializer(Location.class, new LocationDeserializer()));

SqliteBackend backend = new SqliteBackend(new File(getDataFolder(), "myplugin.db"));
DataStore store = new ConcurrentDataStore(
    new ThreadedRepositoryBuilder(
        new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
        Executors.newSingleThreadExecutor(r -> new Thread(r, "myplugin-store-io")),
        mapper));
```

See [`player-data.md`](player-data.md) for a full example using `LocationSerializer`.
