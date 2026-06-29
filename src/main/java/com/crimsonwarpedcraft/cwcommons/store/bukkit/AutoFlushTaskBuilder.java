package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import java.util.Objects;
import org.bukkit.plugin.Plugin;

/**
 * Fluent builder for {@link AutoFlushTask} instances.
 *
 * <p>Obtain one from {@link AutoFlushTask#builder(DataStore, Plugin)}; the interval defaults to
 * {@link AutoFlushTask#DEFAULT_INTERVAL_TICKS} and the post-flush callback to a no-op:
 * <pre>{@code
 * BukkitTask flushTask = AutoFlushTask.builder(store, this)
 *     .interval(12000L)
 *     .build()
 *     .start();
 * }</pre>
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class AutoFlushTaskBuilder {

  private final DataStore store;
  private final Plugin plugin;
  private long intervalTicks = AutoFlushTask.DEFAULT_INTERVAL_TICKS;
  private Runnable onFlush = () -> {};

  AutoFlushTaskBuilder(DataStore store, Plugin plugin) {
    this.store = Objects.requireNonNull(store);
    this.plugin = Objects.requireNonNull(plugin);
  }

  /**
   * Sets how often the store is flushed, in server ticks.
   *
   * @param intervalTicks the flush interval in ticks
   * @return this builder
   */
  public AutoFlushTaskBuilder interval(long intervalTicks) {
    this.intervalTicks = intervalTicks;
    return this;
  }

  /**
   * Sets a callback run on the Bukkit main thread after each flush completes.
   *
   * <p>Because it runs on the main thread, it may safely call the Bukkit API.
   *
   * @param onFlush the post-flush callback
   * @return this builder
   */
  public AutoFlushTaskBuilder onFlush(Runnable onFlush) {
    this.onFlush = Objects.requireNonNull(onFlush);
    return this;
  }

  /**
   * Assembles the configured {@link AutoFlushTask}.
   *
   * @return a task ready to {@link AutoFlushTask#start()}
   */
  public AutoFlushTask build() {
    return new AutoFlushTask(store, plugin, intervalTicks, onFlush);
  }
}
