package com.crimsonwarpedcraft.cwcommons.config;

/**
 * Exception thrown when a config node or group is not found for the provided key.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class ConfigKeyDoesNotExistException extends RuntimeException {
  public ConfigKeyDoesNotExistException(String message) {
    super(message);
  }
}
