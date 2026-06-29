package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import java.util.Objects;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Schedules a periodic {@link DataStore#flush()} via the Bukkit task scheduler.
 *
 * <p>Build one with {@link #builder(DataStore, Plugin)} and call {@link #start()} once in
 * {@code onEnable()}. Store the returned {@link BukkitTask} and cancel it in {@code onDisable()}
 * before calling {@link DataStore#close()}.
 *
 * <p>An optional {@code onFlush} callback is executed on the Bukkit main thread after each
 * flush completes, making it safe to call the Bukkit API (e.g. sending admin notifications).
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class AutoFlushTask {

  /** Default flush interval: 5 minutes at 20 TPS. */
  public static final long DEFAULT_INTERVAL_TICKS = 6000L;

  private final DataStore store;
  private final Plugin plugin;
  private final long intervalTicks;
  private final Runnable onFlush;

  AutoFlushTask(DataStore store, Plugin plugin, long intervalTicks, Runnable onFlush) {
    this.store = Objects.requireNonNull(store);
    this.plugin = Objects.requireNonNull(plugin);
    this.intervalTicks = intervalTicks;
    this.onFlush = Objects.requireNonNull(onFlush);
  }

  /**
   * Returns a builder for an auto-flush task bound to the given store and plugin.
   *
   * @param store the data store to flush
   * @param plugin the owning plugin, used for scheduler access
   * @return a new builder
   */
  public static AutoFlushTaskBuilder builder(DataStore store, Plugin plugin) {
    return new AutoFlushTaskBuilder(store, plugin);
  }

  /**
   * Schedules the periodic flush and returns the underlying {@link BukkitTask}.
   *
   * <p>Cancel the returned task in {@code onDisable()} before closing the store.
   *
   * @return the scheduled task
   */
  public BukkitTask start() {
    return plugin.getServer().getScheduler().runTaskTimer(
        plugin,
        () -> store.flush().thenRun(
            () -> plugin.getServer().getScheduler().runTask(plugin, onFlush)),
        intervalTicks,
        intervalTicks
    );
  }
}
