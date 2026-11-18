package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 * Schedules a periodic {@link DataStore#flush()} via the Bukkit task scheduler.
 *
 * <p>Build one with {@link #builder(DataStore, Plugin)} and call {@link #start()} once in
 * {@code onEnable()}. Store the returned {@link BukkitTask} and cancel it in {@code onDisable()}
 * before calling {@link DataStore#close()}.
 *
 * <p>An optional {@code onFlush} callback is executed on the Bukkit main thread after each
 * successful flush, making it safe to call the Bukkit API (e.g. sending admin notifications).
 * Callbacks are skipped if the plugin is disabled before scheduling or execution. Flush failures
 * are logged even if the plugin has since been disabled.
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
    if (intervalTicks <= 0) {
      throw new IllegalArgumentException("intervalTicks must be positive");
    }
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
    BukkitScheduler scheduler = plugin.getServer().getScheduler();
    Logger logger = plugin.getLogger();
    return scheduler.runTaskTimer(
        plugin,
        () -> flush(scheduler, logger),
        intervalTicks,
        intervalTicks
    );
  }

  private void flush(BukkitScheduler scheduler, Logger logger) {
    CompletableFuture<Void> future;
    try {
      future = store.flush();
    } catch (RuntimeException failure) {
      logFlushFailure(logger, failure);
      return;
    }
    future.handle((ignored, failure) -> {
      if (failure != null) {
        logFlushFailure(logger, failure);
      } else if (plugin.isEnabled()) {
        try {
          scheduler.runTask(plugin, () -> {
            if (plugin.isEnabled()) {
              onFlush.run();
            }
          });
        } catch (RuntimeException schedulingFailure) {
          // Disabling the plugin can race with the enabled check above.
          logger.log(Level.WARNING, "Failed to schedule auto-flush success callback",
              schedulingFailure);
        }
      }
      return null;
    });
  }

  private void logFlushFailure(Logger logger, Throwable failure) {
    logger.log(Level.SEVERE, "Failed to flush data store during periodic auto-flush", failure);
  }
}
