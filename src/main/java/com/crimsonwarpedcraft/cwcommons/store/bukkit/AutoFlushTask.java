package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Schedules a periodic {@link DataStore#flush()} via the Bukkit task scheduler.
 *
 * <p>Construct an instance and call {@link #start()} once in {@code onEnable()}.
 * Store the returned {@link BukkitTask} and cancel it in {@code onDisable()} before
 * calling {@link DataStore#close()}.
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

  /**
   * Creates an {@link AutoFlushTask} that flushes every {@link #DEFAULT_INTERVAL_TICKS} ticks.
   *
   * @param store the data store to flush
   * @param plugin the owning plugin, used for scheduler access
   */
  public AutoFlushTask(DataStore store, Plugin plugin) {
    this(store, plugin, DEFAULT_INTERVAL_TICKS);
  }

  /**
   * Creates an {@link AutoFlushTask} with a custom flush interval.
   *
   * @param store the data store to flush
   * @param plugin the owning plugin, used for scheduler access
   * @param intervalTicks how often to flush, in server ticks
   */
  public AutoFlushTask(DataStore store, Plugin plugin, long intervalTicks) {
    this(store, plugin, intervalTicks, () -> {});
  }

  /**
   * Creates an {@link AutoFlushTask} with a custom flush interval and a post-flush callback.
   *
   * <p>The {@code onFlush} callback is scheduled on the Bukkit main thread after each flush
   * completes, so it may safely call the Bukkit API.
   *
   * @param store the data store to flush
   * @param plugin the owning plugin, used for scheduler access
   * @param intervalTicks how often to flush, in server ticks
   * @param onFlush a callback to run on the main thread after each flush
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
      justification = "DataStore and Runnable are interfaces; defensive copy is not meaningful")
  public AutoFlushTask(DataStore store, Plugin plugin, long intervalTicks, Runnable onFlush) {
    this.store = Objects.requireNonNull(store);
    this.plugin = Objects.requireNonNull(plugin);
    this.intervalTicks = intervalTicks;
    this.onFlush = Objects.requireNonNull(onFlush);
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
