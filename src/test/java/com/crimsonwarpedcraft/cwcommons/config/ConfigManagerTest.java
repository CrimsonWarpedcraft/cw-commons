package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

class ConfigManagerTest {

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class TestConfig implements Config {

    @NotBlank
    @JsonProperty("value")
    private String value = "default";

    TestConfig() {}

    TestConfig(String value) {
      this.value = value;
    }
  }

  @Test
  void loadPropagatesIoException() throws IOException {
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    IOException readFailure = new IOException("read failed");
    doThrow(readFailure)
        .when(mockMapper).readValue(any(File.class), eq(TestConfig.class));
    ConfigManager manager = new ConfigManager(mockMapper, mock(Validator.class));
    IOException error = assertThrows(IOException.class,
        () -> manager.load(new File("anything.yml"), TestConfig.class));
    assertSame(readFailure, error.getCause());
    assertTrue(error.getMessage().contains("anything.yml"));
    assertTrue(error.getMessage().contains("read failed"));
  }

  @Test
  void nullRootReportsFileErrorBeforeValidation() {
    Validator validator = mock(Validator.class);
    ConfigManager manager = new ConfigManager(mock(ObjectMapper.class), validator);
    IOException error = assertThrows(IOException.class,
        () -> manager.load(new File("empty.yml"), TestConfig.class));
    assertTrue(error.getMessage().contains("empty.yml"));
    assertTrue(error.getMessage().contains("null"));
    verifyNoInteractions(validator);
  }

  @Test
  void builderBuildsValidatingManager() {
    try (ConfigManager manager = ConfigManager.builder().build()) {
      assertDoesNotThrow(() -> manager.validate(new TestConfig()));
      assertThrows(IllegalStateException.class, () -> manager.validate(new TestConfig("")));
    }
  }

  @Test
  void validateAcceptsValidConfig() {
    try (ValidatorFactory factory = realValidatorFactory();
        ConfigManager manager = new ConfigManager(new ObjectMapper(), factory.getValidator())) {
      assertDoesNotThrow(() -> manager.validate(new TestConfig()));
    }
  }

  @Test
  void validateThrowsOnViolations() {
    try (ValidatorFactory factory = realValidatorFactory();
        ConfigManager manager = new ConfigManager(new ObjectMapper(), factory.getValidator())) {
      assertThrows(IllegalStateException.class,
          () -> manager.validate(new TestConfig("")));
    }
  }

  @Test
  void loadsConfigFromFile() throws IOException {
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    TestConfig expected = new TestConfig();
    doReturn(expected).when(mockMapper).readValue(any(File.class), eq(TestConfig.class));
    ConfigManager manager = spy(new ConfigManager(mockMapper, mock(Validator.class)));
    doNothing().when(manager).validate(any());
    TestConfig result = manager.load(new File("anything.yml"), TestConfig.class);
    assertSame(expected, result);
    verify(manager).validate(expected);
  }

  @Test
  void loadPropagatesValidationFailure() throws IOException {
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    doReturn(new TestConfig())
        .when(mockMapper).readValue(any(File.class), eq(TestConfig.class));
    ConfigManager manager = spy(new ConfigManager(mockMapper, mock(Validator.class)));
    doThrow(new IllegalStateException("invalid")).when(manager).validate(any());
    assertThrows(IllegalStateException.class,
        () -> manager.load(new File("anything.yml"), TestConfig.class));
  }

  private static ValidatorFactory realValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();
  }
}
