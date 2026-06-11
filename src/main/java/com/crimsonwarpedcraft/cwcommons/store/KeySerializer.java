package com.crimsonwarpedcraft.cwcommons.store;

/**
 * Converts a typed key to and from its string representation for storage.
 *
 * @param <K> the key type
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public interface KeySerializer<K> {

  /**
   * Converts the given key to a string for use as a storage key.
   *
   * @param key the key to serialize
   * @return the string representation of the key
   */
  String serialize(K key);

  /**
   * Restores a key from its string representation.
   *
   * @param raw the string representation of the key
   * @return the deserialized key
   */
  K deserialize(String raw);
}
