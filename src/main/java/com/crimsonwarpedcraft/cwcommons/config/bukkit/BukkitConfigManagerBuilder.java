package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import com.crimsonwarpedcraft.cwcommons.bukkit.serialization.BukkitModule;
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
 * {@link org.bukkit.Location} and {@link org.bukkit.inventory.ItemStack} fields. The core
 * {@link com.crimsonwarpedcraft.cwcommons.config.ConfigManager} stays Bukkit-free; this convenience
 * wires the coupling in for you, the same way {@code BukkitDataStoreBuilder} does for the data
 * store:
 * <pre>{@code
 * MyConfig config = new BukkitConfigManagerBuilder().build().load(configFile, MyConfig.class);
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
