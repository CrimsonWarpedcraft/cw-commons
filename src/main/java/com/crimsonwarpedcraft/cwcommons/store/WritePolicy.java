package com.crimsonwarpedcraft.cwcommons.store;

/**
 * Determines when a {@code CachingBackend} persists writes to its delegate backend.
 *
 * <p>This is the low-level policy the cache is built with. Callers usually pick a
 * {@link CacheMode} on {@link DataStoreBuilder} instead, which selects the matching policy.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public enum WritePolicy {

  /**
   * Buffers writes in memory and persists them on the next flush or store shutdown.
   *
   * <p>This is the default and most efficient policy for single-server deployments.
   */
  CACHE_AND_FLUSH,

  /**
   * Persists each write immediately and atomically through to the backend.
   *
   * <p>Use this when data must survive a process crash without waiting for a flush cycle. The
   * cache still serves local reads, so it does not make a shared database coherent across
   * servers — for that, disable caching with {@link CacheMode#NONE}.
   */
  WRITE_THROUGH_ATOMIC
}
