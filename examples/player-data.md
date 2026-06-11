# Player Data Manager

`PlayerDataManager<V>` wraps a `Repository<UUID, V>` and automatically flushes pending writes
when a player disconnects (`PlayerQuitEvent`). Call `registerEvents()` once in `onEnable()`.

## Setup

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.KeySerializers;
import com.crimsonwarpedcraft.cwcommons.store.Repository;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.PlayerDataManager;
import java.util.UUID;

// Your data type — must be Jackson-serializable
public record PlayerStats(int kills, int deaths) {
    public PlayerStats() { this(0, 0); }
}

// In onEnable()
DataStore store = DataStore.getLocalDataStore("myplugin", getDataFolder());
Repository<UUID, PlayerStats> repo =
    store.repository("stats", PlayerStats.class, KeySerializers.forUuid());
PlayerDataManager<PlayerStats> manager = new PlayerDataManager<>(repo, this);
manager.registerEvents(); // start listening for PlayerQuitEvent
```

## Reading and writing

```java
import java.util.Optional;
import org.bukkit.entity.Player;

// Get (returns Optional — empty if this player has no saved data)
Optional<PlayerStats> stats = manager.get(player).get();
PlayerStats current = stats.orElse(new PlayerStats());

// Save
manager.save(player, new PlayerStats(current.kills() + 1, current.deaths()));
```

## Auto-flush on quit

When `registerEvents()` has been called, `PlayerDataManager` listens for `PlayerQuitEvent`
and calls `repository.flush()` so pending writes are persisted when a player leaves.

For periodic flushing between player quits, pair with `AutoFlushTask`:

```java
import com.crimsonwarpedcraft.cwcommons.store.bukkit.AutoFlushTask;
import org.bukkit.scheduler.BukkitTask;

BukkitTask flushTask = new AutoFlushTask(store, this).start();
```

See [`store-auto-flush.md`](store-auto-flush.md) for the full `AutoFlushTask` API.

## Storing Bukkit types

`Location` and `ItemStack` have custom Jackson serializers in `store/bukkit/`. To use them,
construct the store with a custom `ObjectMapper` (see [`store-sqlite.md`](store-sqlite.md)):

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.LocationDeserializer;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.LocationSerializer;
import org.bukkit.Location;

ObjectMapper mapper = new ObjectMapper().registerModule(new SimpleModule()
    .addSerializer(Location.class, new LocationSerializer())
    .addDeserializer(Location.class, new LocationDeserializer()));
```

## Cleanup in `onDisable()`

```java
// Cancel the flush task, then close the store (flushes all pending writes first)
flushTask.cancel();
store.close();
```
