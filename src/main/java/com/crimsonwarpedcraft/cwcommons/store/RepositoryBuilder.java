package com.crimsonwarpedcraft.cwcommons.store;

/**
 * Creates {@link Repository} instances for use within a {@link DataStore}.
 *
 * <p>Implementations are called by {@link ConcurrentDataStore} to produce per-namespace
 * repositories on demand. The built-in {@link ThreadedRepositoryBuilder} shares a serial I/O lane
 * and lifecycle gate across all repositories: close stops admission, drains, flushes, then releases
 * owned resources. The default {@link #close()} implementation is a no-op.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@FunctionalInterface
public interface RepositoryBuilder {

  /**
   * Creates a {@link Repository} for the given namespace.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param namespace the namespace this repository covers
   * @param type the class of the value type
   * @param keySerializer converts keys to and from their string storage representation
   * @return a new repository
   */
  <K, V> Repository<K, V> create(
      String namespace, Class<V> type, KeySerializer<K> keySerializer);

  /**
   * Releases any resources owned by this builder.
   *
   * <p>Called automatically by {@link DataStore#close()}. The default implementation is a no-op;
   * implementations must not close caller-owned backends or shut down caller-owned executors.
   *
   * @throws java.io.IOException if a resource cannot be released cleanly
   */
  default void close() throws java.io.IOException {}
}
