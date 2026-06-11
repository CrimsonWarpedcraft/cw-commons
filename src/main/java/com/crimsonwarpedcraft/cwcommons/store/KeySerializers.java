package com.crimsonwarpedcraft.cwcommons.store;

import java.util.UUID;

/**
 * Factory methods for common {@link KeySerializer} implementations.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class KeySerializers {

  private KeySerializers() {}

  /**
   * Returns a {@link KeySerializer} for {@link UUID} keys.
   *
   * @return a UUID key serializer
   */
  public static KeySerializer<UUID> forUuid() {
    return new KeySerializer<>() {
      @Override
      public String serialize(UUID key) {
        return key.toString();
      }

      @Override
      public UUID deserialize(String raw) {
        return UUID.fromString(raw);
      }
    };
  }

  /**
   * Returns a {@link KeySerializer} for {@link String} keys.
   *
   * @return a String key serializer
   */
  public static KeySerializer<String> forString() {
    return new KeySerializer<>() {
      @Override
      public String serialize(String key) {
        return key;
      }

      @Override
      public String deserialize(String raw) {
        return raw;
      }
    };
  }
}
