package com.crimsonwarpedcraft.cwcommons.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Objects;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * Fluent builder for {@link ConfigManager} instances.
 *
 * <p>Obtain one from {@link ConfigManager#builder()}; every collaborator has a sensible default, so
 * {@code ConfigManager.builder().build()} is enough for a plain YAML config. Override the
 * {@link #mapper(ObjectMapper) mapper} or {@link #validator(Validator) validator} when you need to:
 * <pre>{@code
 * ConfigManager manager = ConfigManager.builder().build();
 * }</pre>
 *
 * <p>The default mapper reads YAML and binds {@code private} fields; the default validator is the
 * Hibernate validator configured with a {@link ParameterMessageInterpolator} (no EL implementation
 * required). Bukkit plugins should prefer {@code BukkitConfigManagerBuilder}, which also registers
 * the Bukkit (de)serializers so {@code Location}/{@code ItemStack} fields bind.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class ConfigManagerBuilder {

  private ObjectMapper mapper;
  private Validator validator;

  /** Creates a builder that uses the default YAML mapper and Hibernate validator. */
  protected ConfigManagerBuilder() {
  }

  /**
   * Sets the Jackson mapper used to read and bind the config file.
   *
   * @param mapper the Jackson mapper
   * @return this builder
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
      justification = "mapper is intentionally shared by reference; the caller owns it")
  public ConfigManagerBuilder mapper(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper);
    return this;
  }

  /**
   * Sets the Jakarta validator used to check constraints.
   *
   * @param validator the validator
   * @return this builder
   */
  public ConfigManagerBuilder validator(Validator validator) {
    this.validator = Objects.requireNonNull(validator);
    return this;
  }

  /**
   * Assembles the configured {@link ConfigManager}.
   *
   * @return a ready-to-use config manager
   */
  public ConfigManager build() {
    ObjectMapper json = (mapper != null) ? mapper : defaultMapper();
    Validator check = (validator != null) ? validator : defaultValidator();
    return new ConfigManager(json, check);
  }

  private static ObjectMapper defaultMapper() {
    return new ObjectMapper(new YAMLFactory())
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  private static Validator defaultValidator() {
    try (ValidatorFactory factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()) {
      return factory.getValidator();
    }
  }
}
