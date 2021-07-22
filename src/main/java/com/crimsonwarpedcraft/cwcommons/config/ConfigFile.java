package com.crimsonwarpedcraft.cwcommons.config;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Base class for handling YAML config files.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public abstract class ConfigFile {
  private final FileConfiguration config;

  protected ConfigFile(File file) throws ConfigurationException {
    try {
      // Setup defaults
      this.config = new YamlConfiguration();
      Set<ConfigOption> defaults = Objects.requireNonNull(getDefaults());
      this.config.addDefaults(
          configOptionsToYamlConfig(
              defaults
          )
      );

      // Load from file if present
      if (file.exists()) {
        this.config.load(Objects.requireNonNull(file));
      }

      // Validate loaded config
      for (ConfigOption option : defaults) {
        if (!option.getValidityChecker().test(this.config.get(option.getName()))) {
          throw new ConfigurationException(
            String.format(
              "An error occurred while loading config file at %s. The value for the config"
                  + " option \"%s\" is invalid.",
                file.getAbsolutePath(),
                option.getName()
            )
          );
        }
      }

      // Write file out (saves values missing from file)
      this.config.options().copyDefaults(true);
      this.config.save(file);

    } catch (IOException | InvalidConfigurationException e) {
      throw new ConfigurationException(
          "An error occurred while loading config file at " + file.getAbsolutePath(),
          e
      );
    }
  }

  protected FileConfiguration getConfig(){
    return config;
  }

  protected abstract Set<ConfigOption> getDefaults();

  private YamlConfiguration configOptionsToYamlConfig(Set<ConfigOption> options) {
    YamlConfiguration configuration = new YamlConfiguration();

    options.forEach(option -> configuration.set(option.getName(), option.getDefaultValue()));

    return configuration;
  }
}
