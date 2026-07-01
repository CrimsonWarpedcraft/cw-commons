---
title: Bukkit Integration
parent: Examples
nav_order: 4
---

# Bukkit Integration

The `store.bukkit` package connects the generic [Data Store](store.md) to the Bukkit lifecycle so
you don't have to wire it up by hand. It provides three things:

- **[`PlayerDataManager`](#playerdatamanager)** — a `Repository<UUID, V>` wrapper that flushes a
  player's data when they disconnect.
- **[`AutoFlushTask`](#periodic-flushing-with-autoflushtask)** — a scheduled task that flushes the
  whole store on a timer.
- **[`Location` / `ItemStack` serializers](#storing-bukkit-types)** — Jackson (de)serializers for the
  two Bukkit types you most often want to persist.

Together, `PlayerDataManager` and `AutoFlushTask` implement two of the three
[flush triggers](store.md#flushing-and-lifecycle) every plugin should have (the third is
`store.close()` in `onDisable()`).

## PlayerDataManager

`PlayerDataManager<V>` wraps a `Repository<UUID, V>` keyed by player UUID and adds two conveniences:
`get`/`save` that take a `Player` directly, and an automatic flush on `PlayerQuitEvent`.

### Setup

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.KeySerializers;
import com.crimsonwarpedcraft.cwcommons.store.Repository;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.BukkitDataStoreBuilder;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.PlayerDataManager;
import java.util.UUID;

// Your data type — must be Jackson-serializable. A record is the easy choice.
public record PlayerStats(int kills, int deaths) {
    public PlayerStats() { this(0, 0); }
}

// In onEnable()
DataStore store = new BukkitDataStoreBuilder("myplugin", getDataFolder()).build();
Repository<UUID, PlayerStats> repo =
    store.repository("stats", PlayerStats.class, KeySerializers.forUuid());

PlayerDataManager<PlayerStats> manager =
    new PlayerDataManager<>(repo, this).registerEvents();
```

`registerEvents()` registers the manager as a Bukkit listener and returns the manager, so you can
chain it as above. Call it exactly once, in `onEnable()` — until you do, no quit-flush happens.

### Reading and writing

`get`/`save` mirror the [repository API](store.md#reading-and-writing) but take a `Player`. They are
just as asynchronous: the futures complete on the store's I/O thread, so reschedule to the main
thread before touching the Bukkit API.

```java
import org.bukkit.entity.Player;

// Read this player's stats (Optional is empty if they have none saved yet)
manager.get(player).thenAccept(maybeStats -> {
    PlayerStats stats = maybeStats.orElse(new PlayerStats());
    getServer().getScheduler().runTask(this,
        () -> player.sendMessage("Kills: " + stats.kills()));
});

// Save (buffered until the next flush — including the automatic one on quit)
manager.save(player, new PlayerStats(5, 2));
```

### Auto-flush on quit

Once `registerEvents()` has run, the manager listens for `PlayerQuitEvent` and calls
`repository.flush()` when any player leaves. That flush persists **all** buffered writes in the
namespace, not only the departing player's — which is exactly what you want, since it keeps the
write buffer small and bounds data loss to whatever happened since the last quit.

This is why `PlayerDataManager` alone isn't quite enough on a quiet server: if nobody leaves for an
hour, an hour of writes sit in memory. Pair it with [`AutoFlushTask`](#periodic-flushing-with-autoflushtask)
to close that gap.

## Periodic flushing with AutoFlushTask

`AutoFlushTask` schedules a repeating [`DataStore.flush()`](store.md#flushing-and-lifecycle) on the
Bukkit scheduler, so buffered writes are persisted even when no players disconnect. `start()` returns
a `BukkitTask` — keep it so you can cancel it in `onDisable()` before closing the store.

```java
import com.crimsonwarpedcraft.cwcommons.store.bukkit.AutoFlushTask;
import org.bukkit.scheduler.BukkitTask;

// In onEnable()
BukkitTask flushTask = AutoFlushTask.builder(store, this).build().start();

// In onDisable()
flushTask.cancel();
store.close();
```

The default interval is `AutoFlushTask.DEFAULT_INTERVAL_TICKS` — **6000 ticks (5 minutes at 20 TPS)**.
A crash therefore loses at most one interval's worth of writes; shorten the interval to trade a
little extra I/O for a smaller loss window.

### Custom interval

```java
// Flush every 10 minutes (12000 ticks)
BukkitTask flushTask = AutoFlushTask.builder(store, this).interval(12000L).build().start();
```

### Post-flush callback

Pass a `Runnable` to `.onFlush(...)` to run code **on the main thread** after each flush
completes — safe for Bukkit API calls such as sending messages.

```java
BukkitTask flushTask = AutoFlushTask.builder(store, this)
    .onFlush(() -> getServer().getOnlinePlayers().stream()
        .filter(p -> p.hasPermission("myplugin.admin"))
        .forEach(p -> p.sendMessage("[myplugin] Store flushed.")))
    .build()
    .start();
```

### Combining with PlayerDataManager

The two helpers are complementary and safe to use on the same store: `PlayerDataManager` flushes the
moment a player leaves, while `AutoFlushTask` is the safety net for everyone still online.

```java
PlayerDataManager<PlayerStats> manager =
    new PlayerDataManager<>(repo, this).registerEvents();
BukkitTask flushTask = AutoFlushTask.builder(store, this).build().start();
```

## Storing Bukkit types

Bukkit's `Location` and `ItemStack` aren't plain data, so Jackson can't serialize them out of the
box. cw-commons ships (de)serializers for both in the `bukkit.serialization` package, bundled as
`BukkitModule` — see [Bukkit Types](bukkit-types.md) for the serialized format. The data store wires
them in here; config loading reuses the same module via `BukkitConfigManagerBuilder`:

| Type | Serializers | Stored as |
|------|-------------|-----------|
| `Location` | `LocationSerializer` / `LocationDeserializer` | `{"world":…,"x":…,"y":…,"z":…,"yaw":…,"pitch":…}` |
| `ItemStack` | `ItemStackSerializer` / `ItemStackDeserializer` | `{"type":"STICK","amount":64}` (Bukkit `ConfigurationSerializable` map) |

The store persists these as JSON text in SQLite. The same field structure is written as YAML in a
config — see [Bukkit Types](bukkit-types.md) for the format.

> When a stored world is no longer loaded at read time, `LocationDeserializer` returns a `Location`
> whose `world` is `null` — check for it before using the result.

### The easy way: `BukkitDataStoreBuilder`

`BukkitDataStoreBuilder` is the recommended entry point for a Bukkit plugin. It builds a
managed, SQLite-backed store with the same defaults as the [core data store](store.md#quick-start),
but with both (de)serializers already registered — for most plugins it's the only thing you need:

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.BukkitDataStoreBuilder;

// In onEnable()
DataStore store = new BukkitDataStoreBuilder("myplugin", getDataFolder()).build();
```

With the serializers registered store-wide, a `Location` or `ItemStack` anywhere in your value type
just works:

```java
public record HomeData(Location home, ItemStack icon) {}
```

`store.close()` flushes and closes the backend for you — no extra cleanup beyond the usual
`onDisable()` steps [below](#cleanup-in-ondisable).

### Bringing your own mapper

`BukkitDataStoreBuilder` bundles the four (de)serializers as a single Jackson module, `BukkitModule`.
If you build the store with [`DataStore.builder`](store.md#advanced-custom-backends-and-serialization)
— to pick a different cache mode, executor, or backend — register that module on your mapper
instead of wiring the serializers up one by one:

```java
import com.crimsonwarpedcraft.cwcommons.bukkit.serialization.BukkitModule;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper()
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(new BukkitModule());
```

Pass that mapper to `DataStore.builder(backend).mapper(mapper)`. The builder's store owns its backend
by default, so `store.close()` closes the `SqliteBackend` for you; call `.closeBackend(false)` if you
want to keep it open.

### Per-field instead of store-wide

If you'd rather not register globally, annotate individual fields instead — useful when only one
field needs the custom format:

```java
import com.crimsonwarpedcraft.cwcommons.bukkit.serialization.LocationDeserializer;
import com.crimsonwarpedcraft.cwcommons.bukkit.serialization.LocationSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class HomeData {
    @JsonSerialize(using = LocationSerializer.class)
    @JsonDeserialize(using = LocationDeserializer.class)
    private Location home;
}
```

## Cleanup in `onDisable()`

```java
// Cancel the flush task, then close the store (which flushes all pending writes first)
flushTask.cancel();
store.close();
// If you built the store with .closeBackend(false), also close it: backend.close();
```
