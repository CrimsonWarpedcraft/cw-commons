package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crimsonwarpedcraft.cwcommons.mock.NoOpMemorySectionImpl;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class BukkitConfigTest {

  @Test
  void getNewBukkitConfig_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> BukkitConfig.getNewBukkitConfig(null)
    );
  }

  @Test
  void loadFromYaml_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> BukkitConfig.getNewBukkitConfig(ConfigGroup.getNewConfigGroup("test1"))
            .loadFromYamlFile(null)
    );
  }

  @Test
  void loadFromYaml_loads_config_values_properly() {
    YamlConfiguration yaml = new YamlConfiguration() {
      @Override
      public void load(@NotNull File file) {
      }

      @Override
      public @NotNull Map<String, Object> getValues(boolean deep) {
        return Map.of("test2", "test3");
      }
    };

    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
            .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));
    BukkitConfig config = new BukkitConfig(group, yaml);

    assertDoesNotThrow(() -> config.loadFromYamlFile(new File("")));
    assertEquals("test3", config.getConfigGroup().getConfigNode("test2").getValue());
  }

  @Test
  void loadFromYaml_handles_MemorySection_properly() {
    class MockMemorySection extends NoOpMemorySectionImpl {
      private final Map<String, Object> values;

      public MockMemorySection(Map<String, Object> values) {
        this.values = values;
      }

      @Override
      public @NotNull Map<String, Object> getValues(boolean deep) {
        return values;
      }
    }

    YamlConfiguration yaml = new YamlConfiguration() {
      @Override
      public void load(@NotNull File file) {
      }

      @Override
      public @NotNull Map<String, Object> getValues(boolean deep) {
        return Map.of(
            "test2", new MockMemorySection(
                Map.of("test3", new MockMemorySection(Map.of("test4", "test5")))
            )
        );
      }
    };

    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(
            ConfigGroup.getNewConfigGroup("test2")
                .addConfigGroup(
                    ConfigGroup.getNewConfigGroup("test3")
                        .addConfigNode(ConfigNode.getNewConfigNode("test4", String.class, v -> {}))
                )
        );
    BukkitConfig config = new BukkitConfig(group, yaml);

    assertDoesNotThrow(() -> config.loadFromYamlFile(new File("")));
    assertEquals(
        "test5",
        config.getConfigGroup()
            .getConfigGroup("test2")
            .getConfigGroup("test3")
            .getConfigNode("test4")
            .getValue()
    );
  }

  @Test
  void loadFromYaml_throws_IoException_on_read_exception() {
    YamlConfiguration yaml = new YamlConfiguration() {
      @Override
      public void load(@NotNull File file) throws IOException {
        throw new IOException();
      }
    };

    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1");
    BukkitConfig config = new BukkitConfig(group, yaml);

    assertThrows(
        IOException.class,
        () -> config.loadFromYamlFile(new File(""))
    );
  }

  @Test
  void loadFromYaml_throws_ConfigurationException_on_invalid_config() {
    YamlConfiguration yaml = new YamlConfiguration() {
      @Override
      public void load(@NotNull File file) throws InvalidConfigurationException {
        throw new InvalidConfigurationException();
      }
    };

    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1");
    BukkitConfig config = new BukkitConfig(group, yaml);

    assertThrows(
        ConfigurationException.class,
        () -> config.loadFromYamlFile(new File(""))
    );
  }

  @Test
  void saveAsYaml_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> BukkitConfig.getNewBukkitConfig(ConfigGroup.getNewConfigGroup("test1"))
            .saveAsYamlFile(null)
    );
  }

  @Test
  void saveAsYaml_clears_previous_configuration_keys() {
    YamlConfiguration configuration = new YamlConfiguration() {
      @Override
      public void save(@NotNull File file) {
      }
    };
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1");

    configuration.set("test2", "test3");

    BukkitConfig config = new BukkitConfig(group, configuration);

    assertDoesNotThrow(() -> config.saveAsYamlFile(new File("")));
    assertNull(configuration.get("test2"));
  }

  @Test
  void saveAsYaml_sets_values_properly() {
    YamlConfiguration configuration = new YamlConfiguration() {
      @Override
      public void save(@NotNull File file) {
      }
    };
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, "test3", v -> {}));

    BukkitConfig config = new BukkitConfig(group, configuration);

    assertDoesNotThrow(() -> config.saveAsYamlFile(new File("")));
    assertEquals("test3", configuration.get("test2"));
  }

  @Test
  void saveAsYaml_throws_IoException_on_write_exception() {
    YamlConfiguration configuration = new YamlConfiguration() {
      @Override
      public void save(@NotNull File file) throws IOException {
        throw new IOException();
      }
    };
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, "test3", v -> {}));

    BukkitConfig config = new BukkitConfig(group, configuration);

    assertThrows(
        IOException.class,
        () -> config.saveAsYamlFile(new File(""))
    );
  }
}