package com.crimsonwarpedcraft.cwcommons.config;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Options for configuration files.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class ConfigOption {
  private final String name;
  private final Object defaultValue;
  private final Predicate<Object> validityChecker;

  public static ConfigOption getNewConfigOption(String name, Object defaultValue) {
    return getNewConfigOption(name, defaultValue, null);
  }

  public static ConfigOption getNewConfigOption(
      String name,
      Object defaultValue,
      Predicate<Object> validityChecker
  ) {
    return new ConfigOption(name, defaultValue, validityChecker);
  }

  private ConfigOption(String name, Object defaultValue, Predicate<Object> validityChecker) {
    this.name = Objects.requireNonNull(name);
    this.defaultValue = Objects.requireNonNull(defaultValue);
    this.validityChecker = Objects.requireNonNullElseGet(validityChecker, () -> o -> true);

    // Check that the validity checker matches the default value
    if (!this.validityChecker.test(defaultValue)) {
      throw new IllegalArgumentException("Value must match validity checker");
    }
  }

  /** Returns the name of the configuration option. */
  public String getName() {
    return name;
  }

  /** Returns the default value of the configuration option. */
  public Object getDefaultValue() {
    return defaultValue;
  }

  /** Returns the predicate used to validate the option's value. */
  public Predicate<Object> getValidityChecker() {
    return validityChecker;
  }
}
