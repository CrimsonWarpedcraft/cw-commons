package com.crimsonwarpedcraft.cwcommons.store;

import java.util.concurrent.CompletableFuture;

/**
 * A key-value store organized into namespaced {@link Repository} instances.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public interface DataStore extends AutoCloseable {

  /**
   * Returns a {@link Repository} for the given namespace.
   *
   * <p>Calling this method multiple times with the same namespace returns the same instance.
   * Do not reuse the same namespace with different value types.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param namespace a unique name for this collection (e.g. {@code "players"})
   * @param type the class of the value type, used for JSON deserialization
   * @param keySerializer converts keys to and from their string storage representation
   * @return the repository for this namespace
   */
  <K, V> Repository<K, V> repository(
      String namespace, Class<V> type, KeySerializer<K> keySerializer);

  /**
   * Flushes all pending writes in all repositories to the backend.
   *
   * @return a future completing when all dirty entries have been persisted
   */
  CompletableFuture<Void> flush();

  /**
   * Flushes all pending writes and releases any resources owned by the store.
   *
   * <p>Must be called from {@code onDisable()} to avoid data loss.
   *
   * @throws Exception if flush or resource release fails
   */
  @Override
  void close() throws Exception;
}
