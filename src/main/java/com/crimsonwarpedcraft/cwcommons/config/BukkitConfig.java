package com.crimsonwarpedcraft.cwcommons.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Class for handling Bukkit configurations.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class BukkitConfig {
  private final ConfigGroup config;
  private final YamlConfiguration yamlConfiguration;

  /** Returns a new instance of a bukkit config from the provided config group. */
  public static BukkitConfig getNewBukkitConfig(ConfigGroup group) {
    Objects.requireNonNull(group);

    return new BukkitConfig(group);
  }

  protected BukkitConfig(ConfigGroup config) {
    this(config, new YamlConfiguration());
  }

  protected BukkitConfig(ConfigGroup config, YamlConfiguration yamlConfiguration) {
    Objects.requireNonNull(config);
    Objects.requireNonNull(yamlConfiguration);

    this.config = config;
    this.yamlConfiguration = yamlConfiguration;
  }

  /**
   * Loads configuration values from a YAML file.
   *
   * @param file the file to load values from
   * @throws IOException if there is an exception while reading the config file
   * @throws ConfigurationException if the configuration is not valid
   */
  public void loadFromYamlFile(File file) throws IOException, ConfigurationException {
    Objects.requireNonNull(file);

    try {
      yamlConfiguration.load(file);
    } catch (InvalidConfigurationException e) {
      throw new ConfigurationException(
          "Error reading YAML configuration file at " + file.getAbsolutePath(),
          e
      );
    }

    this.config.setValue(normalize(yamlConfiguration.getValues(false)));
  }

  /**
   * Saves this configuration to a YAML file.
   *
   * @param file the file to save the configuration to
   * @throws IOException if there is an exception while writing the config file
   */
  public void saveAsYamlFile(File file) throws IOException {
    Objects.requireNonNull(file);

    yamlConfiguration.getKeys(false).forEach(k -> yamlConfiguration.set(k, null));
    config.asMap().forEach(yamlConfiguration::set);
    yamlConfiguration.save(file);
  }

  private Map<String, Object> normalize(Map<String, Object> map) {
    return map.entrySet()
        .stream()
        .map(
            entry -> {
              // Recursively convert MemorySections to Maps
              if (entry.getValue() instanceof MemorySection m) {
                String key = entry.getKey();
                Object value = normalize(m.getValues(false));

                return new Entry<String, Object>() {
                  @Override
                  public String getKey() {
                    return key;
                  }

                  @Override
                  public Object getValue() {
                    return value;
                  }

                  @Override
                  public Object setValue(Object value) {
                    return null;
                  }
                };
              }

              return entry;
            }
        )
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  protected ConfigGroup getConfigGroup() {
    return config;
  }
}
