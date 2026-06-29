package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
    doThrow(new IOException("read failed"))
        .when(mockMapper).readValue(any(File.class), eq(TestConfig.class));
    ConfigManager manager = new ConfigManager(mockMapper, mock(Validator.class));
    assertThrows(IOException.class,
        () -> manager.load(new File("anything.yml"), TestConfig.class));
  }

  @Test
  void validateAcceptsValidConfig() {
    ConfigManager manager = new ConfigManager(new ObjectMapper(), realValidator());
    assertDoesNotThrow(() -> manager.validate(new TestConfig()));
  }

  @Test
  void validateThrowsOnViolations() {
    ConfigManager manager = new ConfigManager(new ObjectMapper(), realValidator());
    assertThrows(IllegalStateException.class,
        () -> manager.validate(new TestConfig("")));
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

  private static Validator realValidator() {
    try (ValidatorFactory factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()) {
      return factory.getValidator();
    }
  }
}
