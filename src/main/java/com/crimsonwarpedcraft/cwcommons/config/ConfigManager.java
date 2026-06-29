package com.crimsonwarpedcraft.cwcommons.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads and validates plugin configuration from disk.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class ConfigManager {

  private final ObjectMapper mapper;
  private final Validator validator;

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
   * Creates a ConfigManager with the given mapper and validator.
   *
   * @param mapper the Jackson ObjectMapper to use for deserialization
   * @param validator the Jakarta Validator to use for constraint checking
   */
  ConfigManager(ObjectMapper mapper, Validator validator) {
    this.mapper = Objects.requireNonNull(mapper);
    this.validator = Objects.requireNonNull(validator);
  }

  /**
   * Loads and validates the given file as the given config type.
   *
   * @param <T> the config type
   * @param configFile the file to load
   * @param clazz the config class
   * @return the validated configuration
   * @throws IOException if the file cannot be read or parsed
   * @throws IllegalStateException if the config fails validation
   */
  public <T extends Config> T load(File configFile, Class<T> clazz) throws IOException {
    T config = mapper.readValue(Objects.requireNonNull(configFile), Objects.requireNonNull(clazz));
    validate(config);
    return config;
  }

  /**
   * Validates the given configuration object against its declared constraints.
   *
   * @param <T> the config type
   * @param config the configuration to validate
   * @throws IllegalStateException if any constraint is violated
   */
  public <T extends Config> void validate(T config) {
    Set<ConstraintViolation<T>> violations = validator.validate(Objects.requireNonNull(config));
    if (!violations.isEmpty()) {
      String message = violations.stream()
          .map(v -> v.getPropertyPath() + ": " + v.getMessage())
          .collect(Collectors.joining(", "));
      throw new IllegalStateException("Invalid configuration: " + message);
    }
  }
}
