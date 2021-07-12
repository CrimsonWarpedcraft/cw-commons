package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crimsonwarpedcraft.cwcommons.mock.MockConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for ConfigFile.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class ConfigFileTest {
  private static File dataDir;

  @BeforeAll
  static void setUp() {
    dataDir = Paths
        .get(
            System.getProperty("java.io.tmpdir"),
            "CwCommons" + System.currentTimeMillis(),
            "config_data"
        )
        .toFile();

    assertTrue(dataDir.exists() || dataDir.mkdirs());
  }

  @Test
  void getDefaults() throws ConfigurationException, IOException, InvalidConfigurationException {
    File dataFile = new File(dataDir, "config.yml");

    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> new MockConfig(null)
    );

    // Load non-existent config file and make sure defaults load
    MockConfig mockConfig = new MockConfig(dataFile);
    assertEquals("test2", mockConfig.getTestVal());

    // Modify a config value
    YamlConfiguration configuration = new YamlConfiguration();
    configuration.load(dataFile);
    configuration.set("test1", "test3");
    configuration.save(dataFile);

    // Make sure changes are loaded correctly
    mockConfig = new MockConfig(dataFile);
    assertEquals("test3", mockConfig.getTestVal());

    // Make sure validity checking works
    configuration.set("test1", "");
    configuration.save(dataFile);
    assertThrows(
        ConfigurationException.class,
        () -> new MockConfig(dataFile)
    );
  }
}