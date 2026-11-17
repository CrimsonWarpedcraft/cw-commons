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
   * Passes each write to the backend before publishing it in the local cache.
   *
   * <p>With a delegate that persists each save, this avoids waiting for a flush cycle. Durability
   * and atomicity depend on the delegate; a buffering delegate still requires a flush. The cache
   * still serves local reads, so it does not make a shared database coherent across servers — for
   * that, disable caching with {@link CacheMode#NONE}.
   */
  WRITE_THROUGH_ATOMIC
}
