package com.crimsonwarpedcraft.cwcommons.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fluent builder for {@link DataStore} instances — the standard way to assemble a store.
 *
 * <p>Obtain one from {@link DataStore#builder(StorageBackend)}, supplying the required
 * {@link StorageBackend}; set the optional knobs with the fluent setters, then call
 * {@link #build()}:
 * <pre>{@code
 * DataStore store = DataStore.builder(new SqliteBackend(file))
 *     .name("warps")
 *     .cacheMode(CacheMode.CACHE_AND_FLUSH)
 *     .build();
 * }</pre>
 *
 * <p>By default the store buffers writes ({@link CacheMode#CACHE_AND_FLUSH}), dispatches I/O on a
 * single daemon thread named {@code <name>-store-io}, and closes the backend when the store is
 * closed. Call {@link #closeBackend(boolean) closeBackend(false)} to retain ownership of a backend
 * shared elsewhere. Bukkit plugins should prefer {@code BukkitDataStoreBuilder}, which pre-seeds a
 * local SQLite backend and the Bukkit (de)serializers.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class DataStoreBuilder {

  private final StorageBackend backend;
  private String name = "datastore";
  private CacheMode cacheMode = CacheMode.CACHE_AND_FLUSH;
  private ObjectMapper mapper;
  private Executor executor;
  private boolean closeBackend = true;
  private Duration closeTimeout = Duration.ofSeconds(30);

  /**
   * Creates a builder for a store backed by the given backend.
   *
   * @param backend the storage backend the store reads from and writes to
   */
  @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
      justification = "builder holds no security-sensitive state and defines no finalizer; the "
          + "requireNonNull fail-fast on a non-final builder is intentional")
  protected DataStoreBuilder(StorageBackend backend) {
    this.backend = Objects.requireNonNull(backend);
  }

  /**
   * Sets the store name, used for the default I/O thread name ({@code <name>-store-io}).
   *
   * @param name the store name
   * @return this builder
   */
  public DataStoreBuilder name(String name) {
    this.name = Objects.requireNonNull(name);
    return this;
  }

  /**
   * Sets the cache mode applied to every namespace.
   *
   * <p>{@link CacheMode#CACHE_AND_FLUSH} (the default) and {@link CacheMode#WRITE_THROUGH_ATOMIC}
   * cache reads; {@link CacheMode#NONE} disables caching so every read hits the backend fresh —
   * use it when several servers share one database.
   *
   * @param cacheMode whether and how the store caches
   * @return this builder
   */
  public DataStoreBuilder cacheMode(CacheMode cacheMode) {
    this.cacheMode = Objects.requireNonNull(cacheMode);
    return this;
  }

  /**
   * Sets the Jackson mapper used to serialize values to JSON.
   *
   * @param mapper the Jackson mapper
   * @return this builder
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
      justification = "mapper is intentionally shared by reference; the caller owns it")
  public DataStoreBuilder mapper(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper);
    return this;
  }

  /**
   * Sets the executor that all store I/O is dispatched on.
   *
   * <p>When unset, the store owns a single daemon thread named {@code <name>-store-io} and shuts
   * it down on close. A supplied executor remains caller-owned and is never shut down. All store
   * I/O is serialized into one logical lane even with a multi-worker executor. Direct executors
   * such as {@code Runnable::run} run I/O on the caller. Dispatch rejection completes affected
   * requests exceptionally; close reports the rejection and safely attempts terminal cleanup.
   *
   * @param executor the executor to run I/O tasks on
   * @return this builder
   */
  public DataStoreBuilder executor(Executor executor) {
    this.executor = Objects.requireNonNull(executor);
    return this;
  }

  /**
   * Sets whether {@link DataStore#close()} also closes the backend.
   *
   * @param closeBackend {@code true} (the default) to close the backend with the store;
   *     {@code false} to leave it for the caller to close
   * @return this builder
   */
  public DataStoreBuilder closeBackend(boolean closeBackend) {
    this.closeBackend = closeBackend;
    return this;
  }

  /**
   * Sets how long each close call waits for draining, cleanup, and owned worker termination.
   *
   * <p>The default is 30 seconds. Zero checks completion without waiting. Timeout or interruption
   * reports incomplete persistence without cancelling work or closing a backend under active I/O;
   * cleanup stays queued. This bounds waiting, not synchronous execution by a direct executor or
   * terminal cleanup on the caller after rejection. Stuck I/O cannot be forcibly completed.
   *
   * @param closeTimeout a non-negative duration representable in nanoseconds
   * @return this builder
   * @throws IllegalArgumentException if the duration is negative or too large
   */
  public DataStoreBuilder closeTimeout(Duration closeTimeout) {
    Objects.requireNonNull(closeTimeout);
    if (closeTimeout.isNegative()) {
      throw new IllegalArgumentException("closeTimeout must not be negative");
    }
    try {
      closeTimeout.toNanos();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("closeTimeout is too large", e);
    }
    this.closeTimeout = closeTimeout;
    return this;
  }

  /**
   * Assembles the configured {@link DataStore}.
   *
   * @return a ready-to-use store
   */
  public DataStore build() {
    ObjectMapper json = (mapper != null) ? mapper : defaultMapper();
    StorageBackend effective = switch (cacheMode) {
      case NONE -> backend;
      case CACHE_AND_FLUSH -> new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH);
      case WRITE_THROUGH_ATOMIC -> new CachingBackend(backend, WritePolicy.WRITE_THROUGH_ATOMIC);
    };
    ExecutorService ownedExecutor = executor == null ? defaultExecutor(name) : null;
    Executor exec = executor != null ? executor : ownedExecutor;
    return new ConcurrentDataStore(
        new ThreadedRepositoryBuilder(
            effective, exec, json, closeBackend, ownedExecutor, closeTimeout));
  }

  private static ExecutorService defaultExecutor(String name) {
    return Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, name + "-store-io");
      thread.setDaemon(true);
      return thread;
    });
  }

  private static ObjectMapper defaultMapper() {
    return new ObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
}
