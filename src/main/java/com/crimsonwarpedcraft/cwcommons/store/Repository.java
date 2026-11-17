package com.crimsonwarpedcraft.cwcommons.store;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous key-value store for a single document type.
 *
 * <p>Obtain an instance from {@link DataStore#repository}. All operations return
 * {@link CompletableFuture}s; failures are delivered as exceptional completions wrapping the
 * underlying {@link java.io.IOException}.
 *
 * <p>Once the store starts closing, all operations on existing handles return exceptionally
 * completed futures with {@link java.util.concurrent.RejectedExecutionException}. Previously
 * admitted operations are drained; cancelling a returned future does not cancel its I/O.
 *
 * <p><strong>Threading note:</strong> store I/O uses one logical lane, but callback thread affinity
 * is not guaranteed. Non-async {@link CompletableFuture} continuations may run on the completing
 * thread or the caller attaching them, and a direct executor runs I/O on its caller. Any
 * continuation that touches the Bukkit API must be rescheduled explicitly:
 * <pre>{@code
 * repository.get(uuid).thenAccept(data ->
 *     plugin.getServer().getScheduler().runTask(plugin, () ->
 *         player.sendMessage(data.toString())));
 * }</pre>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public interface Repository<K, V> {

  /**
   * Returns the value associated with the given key, or empty if no value exists.
   *
   * @param key the key to look up
   * @return a future completing with the value, or empty
   */
  CompletableFuture<Optional<V>> get(K key);

  /**
   * Returns all key-value pairs currently stored in this repository.
   *
   * @return a future completing with a snapshot of all entries
   */
  CompletableFuture<Map<K, V>> getAll();

  /**
   * Associates the given value with the given key, replacing any existing value.
   *
   * @param key the key to store under
   * @param value the value to store
   * @return a future completing when the write is acknowledged
   */
  CompletableFuture<Void> put(K key, V value);

  /**
   * Removes the value associated with the given key, if any.
   *
   * @param key the key to remove
   * @return a future completing when the deletion is acknowledged
   */
  CompletableFuture<Void> delete(K key);

  /**
   * Flushes all pending writes to the underlying backend immediately.
   *
   * <p>This is a no-op for repositories using {@link CacheMode#WRITE_THROUGH_ATOMIC} or
   * {@link CacheMode#NONE}, which never buffer writes.
   *
   * @return a future completing when all dirty entries have been persisted
   */
  CompletableFuture<Void> flush();
}
