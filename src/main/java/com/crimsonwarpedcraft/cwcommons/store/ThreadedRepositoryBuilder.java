package com.crimsonwarpedcraft.cwcommons.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * A {@link RepositoryBuilder} that creates {@link ThreadedRepository} instances backed by a
 * shared {@link StorageBackend} and serializes all I/O on a provided {@link Executor}.
 *
 * <p>Assembled internally by {@link DataStoreBuilder}; the {@code closeBackend} flag there controls
 * whether {@link #close()} also closes the backend. Use {@link DataStore#builder(StorageBackend)}
 * (or {@code BukkitDataStoreBuilder}) rather than constructing this directly.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class ThreadedRepositoryBuilder implements RepositoryBuilder {

  private final StorageBackend storageBackend;
  private final StoreScheduler scheduler;
  private final ObjectMapper mapper;
  private final boolean closeBackendOnClose;
  private final Set<String> namespaces = ConcurrentHashMap.newKeySet();

  /**
   * Creates a builder that uses the given backend, executor, and mapper.
   *
   * <p>The caller retains ownership of the backend and executor. Close still drains and flushes.
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
    this(storageBackend, executor, mapper, closeBackendOnClose, null, Duration.ofSeconds(30));
  }

  ThreadedRepositoryBuilder(
      StorageBackend storageBackend, Executor executor, ObjectMapper mapper,
      boolean closeBackendOnClose, ExecutorService ownedExecutor, Duration closeTimeout) {
    this.storageBackend = Objects.requireNonNull(storageBackend);
    this.scheduler = new StoreScheduler(
        Objects.requireNonNull(executor), ownedExecutor, closeTimeout);
    this.mapper = Objects.requireNonNull(mapper);
    this.closeBackendOnClose = closeBackendOnClose;
  }

  @Override
  public <K, V> Repository<K, V> create(
      String namespace, Class<V> type, KeySerializer<K> keySerializer) {
    return whileOpen(() -> {
      Objects.requireNonNull(namespace);
      Objects.requireNonNull(type);
      Objects.requireNonNull(keySerializer);
      namespaces.add(namespace);
      return new ThreadedRepository<>(
          namespace, type, keySerializer, storageBackend, scheduler, mapper);
    });
  }

  <T> T whileOpen(Supplier<T> action) {
    return scheduler.whileOpen(action);
  }

  CompletableFuture<Void> flush() {
    return scheduler.submit(() -> {
      IOException failure = null;
      for (String namespace : namespaces) {
        try {
          storageBackend.flush(namespace);
        } catch (IOException e) {
          if (failure == null) {
            failure = e;
          } else if (failure != e) {
            failure.addSuppressed(e);
          }
        }
      }
      if (failure != null) {
        throw failure;
      }
      return null;
    });
  }

  @Override
  public void close() throws IOException {
    scheduler.close(() -> {
      List<Callable<Void>> cleanup = new ArrayList<>();
      for (String namespace : namespaces) {
        cleanup.add(() -> {
          storageBackend.flush(namespace);
          return null;
        });
      }
      if (closeBackendOnClose) {
        cleanup.add(() -> {
          storageBackend.close();
          return null;
        });
      }
      return cleanup;
    });
  }
}
