package com.crimsonwarpedcraft.cwcommons.store;

/**
 * Determines when writes are persisted to the storage backend.
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
   * <p>Use this when data must be durable across nodes or process crashes without waiting
   * for a flush cycle.
   */
  WRITE_THROUGH_ATOMIC
}
