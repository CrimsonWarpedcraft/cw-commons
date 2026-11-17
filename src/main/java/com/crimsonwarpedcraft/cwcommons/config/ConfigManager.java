package com.crimsonwarpedcraft.cwcommons.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads and validates plugin configuration from disk.
 *
 * <p>Close the manager after its last use to release its internally created validator factory.
 * A validator supplied to the builder remains caller-owned; its factory must stay open until all
 * users have finished. Closing this manager never closes an externally supplied validator's
 * factory. Loaded configuration objects remain usable after the manager is closed.
 *
 * <p>Calls to {@link #load}, {@link #validate}, and {@link #close} are serialized on this manager.
 * Closing waits for an in-flight call to finish; later load/validate calls fail. Do not close the
 * manager from a mapper or validation callback, or mutate shared collaborators during use.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class ConfigManager implements AutoCloseable {

  private final ObjectMapper mapper;
  private final Validator validator;
  private final ValidatorFactory ownedValidatorFactory;
  private boolean closed;

  /**
   * Returns a fluent builder for a config manager.
   *
   * <p>This is the standard entry point. Bukkit plugins should prefer
   * {@code BukkitConfigManagerBuilder}, which also registers the Bukkit (de)serializers so
   * {@code Location}/{@code ItemStack} fields bind from YAML.
   *
   * @return a new builder
   */
  public static ConfigManagerBuilder builder() {
    return new ConfigManagerBuilder();
  }

  /**
   * Creates a non-owning ConfigManager with the given mapper and validator.
   *
   * @param mapper the Jackson ObjectMapper to use for deserialization
   * @param validator the Jakarta Validator to use for constraint checking
   */
  ConfigManager(ObjectMapper mapper, Validator validator) {
    this(mapper, validator, null);
  }

  ConfigManager(ObjectMapper mapper, Validator validator, ValidatorFactory ownedValidatorFactory) {
    this.mapper = Objects.requireNonNull(mapper);
    this.validator = Objects.requireNonNull(validator);
    this.ownedValidatorFactory = ownedValidatorFactory;
  }

  /**
   * Loads and validates the given file as the given config type.
   *
   * @param <T> the config type
   * @param configFile the file to load
   * @param clazz the config class
   * @return the validated configuration
   * @throws IOException if the file cannot be read or parsed
   * @throws IllegalStateException if the manager is closed or the config fails validation
   */
  public synchronized <T extends Config> T load(File configFile, Class<T> clazz)
      throws IOException {
    ensureOpen();
    T config = mapper.readValue(Objects.requireNonNull(configFile), Objects.requireNonNull(clazz));
    validate(config);
    return config;
  }

  /**
   * Validates the given configuration object against its declared constraints.
   *
   * @param <T> the config type
   * @param config the configuration to validate
   * @throws IllegalStateException if the manager is closed or any constraint is violated
   */
  public synchronized <T extends Config> void validate(T config) {
    ensureOpen();
    Set<ConstraintViolation<T>> violations = validator.validate(Objects.requireNonNull(config));
    if (!violations.isEmpty()) {
      String message = violations.stream()
          .map(v -> v.getPropertyPath() + ": " + v.getMessage())
          .collect(Collectors.joining(", "));
      throw new IllegalStateException("Invalid configuration: " + message);
    }
  }

  /**
   * Closes the internally created validator factory, if any, and rejects further use.
   *
   * <p>This is idempotent, including when the factory's first close attempt throws. Externally
   * supplied validators and mappers are never closed.
   */
  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    if (ownedValidatorFactory != null) {
      ownedValidatorFactory.close();
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("ConfigManager is closed");
    }
  }
}
