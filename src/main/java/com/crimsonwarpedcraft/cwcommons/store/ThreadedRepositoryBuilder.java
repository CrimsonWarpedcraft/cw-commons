package com.crimsonwarpedcraft.cwcommons.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A {@link RepositoryBuilder} that creates {@link ThreadedRepository} instances backed by a
 * shared {@link StorageBackend} and dispatches all I/O to a provided {@link Executor}.
 *
 * <p>Assembled internally by {@link DataStoreBuilder}; the {@code closeBackend} flag there controls
 * whether {@link #close()} also closes the backend. Use {@link DataStore#builder(StorageBackend)}
 * (or {@code BukkitDataStoreBuilder}) rather than constructing this directly.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class ThreadedRepositoryBuilder implements RepositoryBuilder {

  private final StorageBackend storageBackend;
  private final Executor executor;
  private final ObjectMapper mapper;
  private final boolean closeBackendOnClose;

  /**
   * Creates a builder that uses the given backend, executor, and mapper.
   *
   * <p>The caller retains ownership of {@code storageBackend}; {@link #close()} is a no-op.
   *
   * @param storageBackend the backend to read from and write to
   * @param executor the executor to dispatch I/O tasks on
   * @param mapper the Jackson mapper used for JSON serialization
   */
  ThreadedRepositoryBuilder(
      StorageBackend storageBackend, Executor executor, ObjectMapper mapper) {
    this(storageBackend, executor, mapper, false);
  }

  /**
   * Creates a builder that uses the given backend, executor, and mapper, optionally taking
   * ownership of the backend.
   *
   * @param storageBackend the backend to read from and write to
   * @param executor the executor to dispatch I/O tasks on
   * @param mapper the Jackson mapper used for JSON serialization
   * @param closeBackendOnClose if {@code true}, {@link #close()} also closes the backend;
   *     {@code false} leaves it for the caller to close
   */
  ThreadedRepositoryBuilder(
      StorageBackend storageBackend, Executor executor, ObjectMapper mapper,
      boolean closeBackendOnClose) {
    this.storageBackend = Objects.requireNonNull(storageBackend);
    this.executor = Objects.requireNonNull(executor);
    this.mapper = Objects.requireNonNull(mapper);
    this.closeBackendOnClose = closeBackendOnClose;
  }

  @Override
  public <K, V> Repository<K, V> create(
      String namespace, Class<V> type, KeySerializer<K> keySerializer) {
    return new ThreadedRepository<>(
        namespace, type, keySerializer, storageBackend, executor, mapper);
  }

  @Override
  public void close() throws IOException {
    if (closeBackendOnClose) {
      storageBackend.close();
    }
  }
}
