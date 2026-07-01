package com.crimsonwarpedcraft.cwcommons.store;

/**
 * Selects how a {@link DataStore} caches, chosen on {@link DataStoreBuilder#cacheMode(CacheMode)}.
 *
 * <p>The two cached modes wrap the backend in a {@code CachingBackend} with the corresponding
 * {@link WritePolicy}; {@link #NONE} skips that layer entirely.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public enum CacheMode {

  /**
   * Disables the in-memory cache entirely — every read hits the backend and every write goes
   * straight through.
   *
   * <p>Use this when several servers share one database, so each node always sees the others'
   * latest writes. {@code flush()} is a no-op because nothing is buffered.
   */
  NONE,

  /**
   * Caches reads and buffers writes in memory, persisting them on the next flush or store shutdown.
   *
   * <p>This is the default and most efficient mode for single-server deployments. Maps to
   * {@link WritePolicy#CACHE_AND_FLUSH}.
   */
  CACHE_AND_FLUSH,

  /**
   * Caches reads but persists each write immediately and atomically through to the backend.
   *
   * <p>Use this when data must survive a crash without waiting for a flush cycle. Note the cache
   * still serves local reads, so on a shared database use {@link #NONE} instead. Maps to
   * {@link WritePolicy#WRITE_THROUGH_ATOMIC}.
   */
  WRITE_THROUGH_ATOMIC
}
