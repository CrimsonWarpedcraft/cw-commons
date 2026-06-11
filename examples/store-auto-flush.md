# Auto-Flush Task

`AutoFlushTask` schedules a periodic `DataStore.flush()` via the Bukkit task scheduler,
ensuring buffered writes are persisted even if no players disconnect between flushes.

The default interval is 5 minutes (6000 ticks). `start()` returns a `BukkitTask` so you
can cancel it in `onDisable()` before closing the store.

## Basic usage

```java
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.bukkit.AutoFlushTask;
import org.bukkit.scheduler.BukkitTask;

// In onEnable()
DataStore store = DataStore.getLocalDataStore("myplugin", getDataFolder());
BukkitTask flushTask = new AutoFlushTask(store, this).start();

// In onDisable()
flushTask.cancel();
store.close();
```

## Custom interval

```java
// Flush every 10 minutes (12000 ticks)
BukkitTask flushTask = new AutoFlushTask(store, this, 12000L).start();
```

## Post-flush callback

Pass a `Runnable` as the fourth argument to run code on the Bukkit main thread after
each flush completes. This is safe for Bukkit API calls such as sending messages.

```java
BukkitTask flushTask = new AutoFlushTask(store, this, AutoFlushTask.DEFAULT_INTERVAL_TICKS,
    () -> getServer().getOnlinePlayers().stream()
        .filter(p -> p.hasPermission("myplugin.admin"))
        .forEach(p -> p.sendMessage("[myplugin] Store flushed.")))
    .start();
```

## Combining with PlayerDataManager

`AutoFlushTask` and `PlayerDataManager` complement each other: `PlayerDataManager` flushes
on every `PlayerQuitEvent`, while `AutoFlushTask` provides a safety net for data written
between quits.

```java
// Both are safe to use on the same DataStore / Repository
PlayerDataManager<PlayerStats> manager = new PlayerDataManager<>(repo, this);
manager.registerEvents();

BukkitTask flushTask = new AutoFlushTask(store, this).start();
```
