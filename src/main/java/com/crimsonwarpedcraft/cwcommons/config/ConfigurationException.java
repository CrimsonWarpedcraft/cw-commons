package com.crimsonwarpedcraft.cwcommons.config;

import java.util.Objects;

/**
 * Exception thrown when there is an issue reading a config file.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class ConfigurationException extends Exception {
  public ConfigurationException(String msg) {
    super(Objects.requireNonNull(msg));
  }

  public ConfigurationException(String msg, Throwable throwable) {
    super(Objects.requireNonNull(msg), Objects.requireNonNull(throwable));
  }
}
