package com.crimsonwarpedcraft.cwcommons.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * A write-behind {@link DataStore} that buffers writes in memory and flushes them to a
 * {@link StorageBackend} asynchronously via the {@link RepositoryBuilder}'s executor.
 *
 * <p>Construct one via {@link DataStore#builder(StorageBackend)} (or {@code BukkitDataStoreBuilder}
 * for Bukkit plugins), which wires up the backend, executor, and mapper for you.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class ConcurrentDataStore implements DataStore {

  private final RepositoryBuilder repositoryBuilder;
  private final ConcurrentHashMap<String, Repository<?, ?>> repositories =
      new ConcurrentHashMap<>();

  /**
   * Creates a store that delegates repository creation to {@code repositoryBuilder}.
   *
   * @param repositoryBuilder the factory used to create per-namespace repositories
   */
  ConcurrentDataStore(RepositoryBuilder repositoryBuilder) {
    this.repositoryBuilder = Objects.requireNonNull(repositoryBuilder);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Repository<K, V> repository(
      String namespace, Class<V> type, KeySerializer<K> keySerializer) {
    Objects.requireNonNull(namespace);
    Objects.requireNonNull(type);
    Objects.requireNonNull(keySerializer);
    return (Repository<K, V>) repositories.computeIfAbsent(
        namespace, k -> repositoryBuilder.create(k, type, keySerializer));
  }

  @Override
  public CompletableFuture<Void> flush() {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    repositories.values().forEach(r -> futures.add(r.flush()));
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }

  @Override
  public void close() throws IOException {
    try {
      flush().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof IOException ioex) {
        throw ioex;
      }
      throw new IOException("Flush failed during store close", e.getCause());
    } finally {
      repositoryBuilder.close();
    }
  }
}
