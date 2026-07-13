package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigManagerIntegrationTest {

  @TempDir
  private Path tempDir;

  @Test
  void loadsYamlAndValidatesBoundFields() throws IOException {
    Path configFile = writeConfig("message: hello\nretries: 3\n");

    TestConfig config =
        ConfigManager.builder().build().load(configFile.toFile(), TestConfig.class);

    assertEquals("hello", config.message);
    assertEquals(3, config.retries);
  }

  @Test
  void rejectsYamlThatViolatesConstraints() throws IOException {
    Path configFile = writeConfig("message: '  '\nretries: 0\n");

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> ConfigManager.builder().build().load(configFile.toFile(), TestConfig.class));

    assertTrue(exception.getMessage().contains("message"));
    assertTrue(exception.getMessage().contains("retries"));
  }

  @Test
  void rejectsMalformedYaml() throws IOException {
    Path configFile = writeConfig("message: [unterminated\n");

    assertThrows(IOException.class,
        () -> ConfigManager.builder().build().load(configFile.toFile(), TestConfig.class));
  }

  private Path writeConfig(String yaml) throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    Files.writeString(configFile, yaml);
    return configFile;
  }

  static final class TestConfig implements Config {

    @NotBlank
    private String message;

    @Min(1)
    private int retries;
  }
}
