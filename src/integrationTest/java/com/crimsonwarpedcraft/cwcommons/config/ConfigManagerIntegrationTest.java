package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigManagerIntegrationTest {

  @TempDir
  private Path tempDir;

  @Test
  void loadsYamlAndValidatesBoundFields() throws IOException {
    Path configFile = writeConfig("message: hello\nretries: 3\n");

    TestConfig config;
    try (ConfigManager manager = ConfigManager.builder().build()) {
      config = manager.load(configFile.toFile(), TestConfig.class);
    }

    assertEquals("hello", config.message);
    assertEquals(3, config.retries);
  }

  @Test
  void rejectsYamlThatViolatesConstraints() throws IOException {
    Path configFile = writeConfig("message: '  '\nretries: 0\n");

    try (ConfigManager manager = ConfigManager.builder().build()) {
      IllegalStateException exception = assertThrows(IllegalStateException.class,
          () -> manager.load(configFile.toFile(), TestConfig.class));

      assertTrue(exception.getMessage().contains("message"));
      assertTrue(exception.getMessage().contains("retries"));
    }
  }

  @Test
  void rejectsMalformedYaml() throws IOException {
    Path configFile = writeConfig("message: [unterminated\n");

    try (ConfigManager manager = ConfigManager.builder().build()) {
      IOException error = assertThrows(IOException.class,
          () -> manager.load(configFile.toFile(), TestConfig.class));
      assertTrue(error.getMessage().contains(configFile.toString()));
      assertInstanceOf(JsonProcessingException.class, error.getCause());
      assertTrue(error.getMessage().contains("line"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "# comment only\n", "null\n", "~\n", "[one, two]\n", "true\n"})
  void rejectsEmptyNullOrWrongShapeWithFileContext(String yaml) throws IOException {
    Path file = writeConfig(yaml);
    try (ConfigManager manager = ConfigManager.builder().build()) {
      IOException error = assertThrows(IOException.class,
          () -> manager.load(file.toFile(), TestConfig.class));
      assertTrue(error.getMessage().contains(file.toString()));
      assertTrue(error.getMessage().contains("TestConfig"));
    }
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
