package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import com.crimsonwarpedcraft.cwcommons.bukkit.serialization.BukkitModule;
import com.crimsonwarpedcraft.cwcommons.config.ConfigManager;
import com.crimsonwarpedcraft.cwcommons.config.ConfigManagerBuilder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A {@link ConfigManagerBuilder} pre-wired for Bukkit plugins — the standard entry point for
 * loading a config.
 *
 * <p>It registers {@link BukkitModule} on the YAML mapper, so config classes can bind
 * {@link org.bukkit.Location} and {@link org.bukkit.inventory.ItemStack} fields. In version 0.4.0
 * and later, use try-with-resources for a one-time load; the returned config remains usable:
 * <pre>{@code
 * MyConfig config;
 * try (ConfigManager manager = new BukkitConfigManagerBuilder().build()) {
 *   config = manager.load(configFile, MyConfig.class);
 * }
 * }</pre>
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class BukkitConfigManagerBuilder extends ConfigManagerBuilder {

  /** Creates a builder whose YAML mapper has the Bukkit (de)serializers registered. */
  public BukkitConfigManagerBuilder() {
    mapper(defaultMapper().registerModule(new BukkitModule()));
  }

  private static ObjectMapper defaultMapper() {
    return new ObjectMapper(new YAMLFactory())
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }
}
