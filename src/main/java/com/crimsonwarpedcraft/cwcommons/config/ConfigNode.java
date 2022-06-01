package com.crimsonwarpedcraft.cwcommons.config;

import java.util.Objects;

/**
 * Value node for a config tree.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class ConfigNode<T> {
  private final ValidityChecker<T> validityChecker;
  private final T defaultValue;
  private final String name;
  private final Class<T> cls;
  private T value;

  /**
   * Get a new instance of a config node.
   *
   * @param name the name of this value
   * @param cls a class representing the type of this node
   * @param validityChecker the validity checker to verify this node's value
   * @param <T> the type of this node
   * @return a new instance of a config node
   */
  public static <T> ConfigNode<T> getNewConfigNode(
      String name,
      Class<T> cls,
      ValidityChecker<T> validityChecker
  ) {
    return getNewConfigNode(name, cls, null, validityChecker);
  }

  /**
   * Get a new instance of a config node.
   *
   * @param name the name of this value
   * @param cls a class representing the type of this node
   * @param defaultValue the default value for this node
   * @param validityChecker the validity checker to verify this node's value
   * @param <T> the type of this node
   * @return a new instance of a config node
   */
  public static <T> ConfigNode<T> getNewConfigNode(
      String name,
      Class<T> cls,
      T defaultValue,
      ValidityChecker<T> validityChecker
  ) {
    return new ConfigNode<>(name, cls, defaultValue, validityChecker);
  }

  protected ConfigNode(
      String name,
      Class<T> cls,
      T defaultValue,
      ValidityChecker<T> validityChecker
  ) {
    Objects.requireNonNull(cls);
    Objects.requireNonNull(name);
    Objects.requireNonNull(validityChecker);

    this.cls = cls;
    this.defaultValue = defaultValue;
    this.name = name;
    this.validityChecker = validityChecker;
  }

  private T validate(T value) throws ConfigurationException {
    validityChecker.validate(value);

    return value;
  }

  /** Returns the name of this config value. */
  public String getName() {
    return name;
  }

  /**
   * Returns the value of this config node.
   *
   * @param <U> the type to cast the value to
   * @return the value of this config node or the default value if value is null
   * @throws ClassCastException if the value cannot be cast to the desired type
   */
  public <U> U getValue() throws ClassCastException {
    if (value == null) {
      return (U) defaultValue;
    }

    return (U) value;
  }

  /**
   * Sets the value of this config node.
   *
   * @param value the value to set this config node to
   * @throws ConfigurationException if the value is invalid
   */
  public void setValue(Object value) throws ConfigurationException {
    if (value != null && !cls.isAssignableFrom(value.getClass())) {
      throw new ConfigurationException(
          String.format(
              "Expected config value for %s to be %s. Found %s.",
              getName(),
              cls.getSimpleName(),
              value.getClass().getSimpleName()
          )
      );
    }

    this.value = validate((T) value);
  }
}
