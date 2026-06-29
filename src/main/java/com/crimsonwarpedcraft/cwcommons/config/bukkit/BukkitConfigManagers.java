package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import com.crimsonwarpedcraft.cwcommons.bukkit.serialization.BukkitModule;
import com.crimsonwarpedcraft.cwcommons.config.ConfigManager;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * Factory for {@link ConfigManager} instances pre-wired for Bukkit plugins — the standard entry
 * point for loading a config.
 *
 * <p>It registers {@link BukkitModule} on the config mapper, so config classes can bind
 * {@link org.bukkit.Location} and {@link org.bukkit.inventory.ItemStack} fields from YAML. The core
 * {@link ConfigManager} stays Bukkit-free; this convenience wires the coupling in for you, the same
 * way {@code BukkitDataStores} does for the data store.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class BukkitConfigManagers {

  private BukkitConfigManagers() {
  }

  /**
   * Creates a {@link ConfigManager} with the Bukkit (de)serializers registered.
   *
   * @return a ConfigManager that can load {@code Location} and {@code ItemStack} values from config
   */
  public static ConfigManager create() {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .registerModule(new BukkitModule());
    return new ConfigManager(mapper, buildValidator());
  }

  private static Validator buildValidator() {
    try (ValidatorFactory factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()) {
      return factory.getValidator();
    }
  }
}
