package com.crimsonwarpedcraft.cwcommons.store;

import java.util.concurrent.CompletableFuture;

/**
 * A key-value store organized into namespaced {@link Repository} instances.
 *
 * <p>Stores built by {@link #builder(StorageBackend)} admit requests only while open. Starting
 * close rejects new repository lookups and new operations, including through existing handles.
 * Already admitted operations drain in one logical I/O lane before terminal cleanup.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public interface DataStore extends AutoCloseable {

  /**
   * Returns a fluent builder for a store backed by the given backend.
   *
   * <p>This is the standard entry point for assembling a store. Bukkit plugins should prefer
   * {@code BukkitDataStoreBuilder}, which pre-seeds a local SQLite backend and the Bukkit
   * (de)serializers.
   *
   * @param backend the storage backend (e.g. {@link SqliteBackend} or {@code MongoDbBackend})
   * @return a new builder
   */
  static DataStoreBuilder builder(StorageBackend backend) {
    return new DataStoreBuilder(backend);
  }

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
   * @throws java.util.concurrent.RejectedExecutionException if closure has started
   */
  <K, V> Repository<K, V> repository(
      String namespace, Class<V> type, KeySerializer<K> keySerializer);

  /**
   * Flushes all pending writes in all repositories to the backend.
   *
   * @return a future completing when all dirty entries have been persisted, or exceptionally
   *     if closure has started or dispatch/I/O fails
   */
  CompletableFuture<Void> flush();

  /**
   * Flushes all pending writes and releases any resources owned by the store.
   *
   * <p>Call from the lifecycle owner (for Bukkit, {@code onDisable()}), not from a store operation
   * or an inline continuation running inside that operation. Blocking close from the store's own
   * execution context fails immediately instead of deadlocking.
   *
   * <p>Close is idempotent and safe for concurrent callers. It stops admission, drains previously
   * admitted work, flushes every namespace, closes the backend if owned, then shuts down the owned
   * default executor and waits for its termination. Caller-supplied executors are never shut down;
   * {@link DataStoreBuilder#closeBackend(boolean) closeBackend(false)} retains the backend.
   * Earlier operation failures are reported along with suppressed cleanup failures.
   *
   * <p>Each caller waits up to {@link DataStoreBuilder#closeTimeout(java.time.Duration)}
   * (30 seconds by default). Timeout or interruption throws an {@link java.io.IOException}
   * reporting incomplete persistence/resource release; interruption preserves interrupt status.
   * Unfinished cleanup remains queued:
   * close never cancels queued writes or closes the backend under active I/O. A later close can
   * wait again. Stuck backend I/O cannot be forcibly guaranteed to finish. The wait limit cannot
   * preempt synchronous execution by a direct executor, or caller-side cleanup after rejection.
   *
   * @throws Exception if an operation, flush, resource release, or close wait fails
   */
  @Override
  void close() throws Exception;
}
