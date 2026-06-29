package com.crimsonwarpedcraft.cwcommons.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.concurrent.Executor;
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
 *     .writePolicy(WritePolicy.CACHE_AND_FLUSH)
 *     .build();
 * }</pre>
 *
 * <p>By default the store buffers writes ({@link WritePolicy#CACHE_AND_FLUSH}), dispatches I/O on a
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
  private WritePolicy writePolicy = WritePolicy.CACHE_AND_FLUSH;
  private ObjectMapper mapper;
  private Executor executor;
  private boolean closeBackend = true;

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
   * Sets the write policy applied to every namespace.
   *
   * @param writePolicy when writes are persisted to the backend
   * @return this builder
   */
  public DataStoreBuilder writePolicy(WritePolicy writePolicy) {
    this.writePolicy = Objects.requireNonNull(writePolicy);
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
   * <p>When unset, a single daemon thread named {@code <name>-store-io} is used.
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
   * Assembles the configured {@link DataStore}.
   *
   * @return a ready-to-use store
   */
  public DataStore build() {
    CachingBackend caching = new CachingBackend(backend, writePolicy);
    Executor exec = (executor != null) ? executor : defaultExecutor(name);
    ObjectMapper json = (mapper != null) ? mapper : defaultMapper();
    return new ConcurrentDataStore(
        new ThreadedRepositoryBuilder(caching, exec, json, closeBackend));
  }

  private static Executor defaultExecutor(String name) {
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
