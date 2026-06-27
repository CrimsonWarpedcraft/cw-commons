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
import com.crimsonwarpedcraft.cwcommons.store.bukkit.PlayerDataManager;
import java.util.UUID;

// Your data type — must be Jackson-serializable. A record is the easy choice.
public record PlayerStats(int kills, int deaths) {
    public PlayerStats() { this(0, 0); }
}

// In onEnable()
DataStore store = DataStore.getLocalDataStore("myplugin", getDataFolder());
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
BukkitTask flushTask = new AutoFlushTask(store, this).start();

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
BukkitTask flushTask = new AutoFlushTask(store, this, 12000L).start();
```

### Post-flush callback

Pass a `Runnable` as the fourth argument to run code **on the main thread** after each flush
completes — safe for Bukkit API calls such as sending messages.

```java
BukkitTask flushTask = new AutoFlushTask(store, this, AutoFlushTask.DEFAULT_INTERVAL_TICKS,
    () -> getServer().getOnlinePlayers().stream()
        .filter(p -> p.hasPermission("myplugin.admin"))
        .forEach(p -> p.sendMessage("[myplugin] Store flushed.")))
    .start();
```

### Combining with PlayerDataManager

The two helpers are complementary and safe to use on the same store: `PlayerDataManager` flushes the
moment a player leaves, while `AutoFlushTask` is the safety net for everyone still online.

```java
PlayerDataManager<PlayerStats> manager =
    new PlayerDataManager<>(repo, this).registerEvents();
BukkitTask flushTask = new AutoFlushTask(store, this).start();
```

## Storing Bukkit types

Bukkit's `Location` and `ItemStack` aren't plain data, so Jackson can't serialize them out of the
box. The package ships (de)serializers for both:

| Type | Serializers | Stored as |
|------|-------------|-----------|
| `Location` | `LocationSerializer` / `LocationDeserializer` | `{"world":…,"x":…,"y":…,"z":…,"yaw":…,"pitch":…}` |
| `ItemStack` | `ItemStackSerializer` / `ItemStackDeserializer` | Base64 of `ItemStack#serializeAsBytes()` (preserves all NBT) |

> When a stored world is no longer loaded at read time, `LocationDeserializer` returns a `Location`
> whose `world` is `null` — check for it before using the result.

To use them, register them on a custom `ObjectMapper` and build the store by hand (this replaces
`getLocalDataStore`, which builds its own mapper you can't extend — see
[Advanced: custom backends](store.md#advanced-custom-backends-and-serialization)):

```java
import com.crimsonwarpedcraft.cwcommons.store.CachingBackend;
import com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStore;
import com.crimsonwarpedcraft.cwcommons.store.SqliteBackend;
import com.crimsonwarpedcraft.cwcommons.store.ThreadedRepositoryBuilder;
import com.crimsonwarpedcraft.cwcommons.store.WritePolicy;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.ItemStackDeserializer;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.ItemStackSerializer;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.LocationDeserializer;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.LocationSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.util.concurrent.Executors;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

ObjectMapper mapper = new ObjectMapper()
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(new SimpleModule()
        .addSerializer(Location.class, new LocationSerializer())
        .addDeserializer(Location.class, new LocationDeserializer())
        .addSerializer(ItemStack.class, new ItemStackSerializer())
        .addDeserializer(ItemStack.class, new ItemStackDeserializer()));

SqliteBackend backend = new SqliteBackend(new File(getDataFolder(), "myplugin.db"));
DataStore store = new ConcurrentDataStore(
    new ThreadedRepositoryBuilder(
        new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
        Executors.newSingleThreadExecutor(r -> new Thread(r, "myplugin-store-io")),
        mapper));
```

With those serializers registered module-wide, a `Location` or `ItemStack` anywhere in your value
type just works:

```java
public record HomeData(Location home, ItemStack icon) {}
```

If you'd rather not register them globally, annotate individual fields instead — useful when only one
field needs the custom format:

```java
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class HomeData {
    @JsonSerialize(using = LocationSerializer.class)
    @JsonDeserialize(using = LocationDeserializer.class)
    private Location home;
}
```

Because this store is hand-built, remember the [ownership rule](store.md#advanced-custom-backends-and-serialization):
`store.close()` won't close the `SqliteBackend`, so close it yourself in `onDisable()` (or wrap the
construction in `getLocalDataStore` if you don't need the custom mapper).

## Cleanup in `onDisable()`

```java
// Cancel the flush task, then close the store (which flushes all pending writes first)
flushTask.cancel();
store.close();
// If you built the store by hand with your own backend, also: backend.close();
```
