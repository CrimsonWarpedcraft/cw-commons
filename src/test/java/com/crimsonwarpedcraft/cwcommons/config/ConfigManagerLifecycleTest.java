package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Configuration;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.bootstrap.GenericBootstrap;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ConfigManagerLifecycleTest {

  private final ObjectMapper mapper = mock(ObjectMapper.class);
  private final ValidatorFactory factory = mock(ValidatorFactory.class);
  private final Validator validator = mock(Validator.class);
  private MockedStatic<Validation> validation;

  record TestConfig() implements Config {}

  @BeforeEach
  void setUp() {
    GenericBootstrap bootstrap = mock(GenericBootstrap.class);
    Configuration<?> configuration = mock(Configuration.class, RETURNS_SELF);
    doReturn(configuration).when(bootstrap).configure();
    when(configuration.buildValidatorFactory()).thenReturn(factory);
    when(factory.getValidator()).thenReturn(validator);
    validation = mockStatic(Validation.class);
    validation.when(Validation::byDefaultProvider).thenReturn(bootstrap);
  }

  @AfterEach
  void tearDown() {
    validation.close();
  }

  @Test
  void defaultFactoryStaysOpenThroughoutLoadAndValidation() throws IOException {
    TestConfig config = new TestConfig();
    File file = new File("mocked.yml");
    when(mapper.readValue(file, TestConfig.class)).thenReturn(config);
    when(validator.validate(config)).thenAnswer(invocation -> {
      verify(factory, never()).close();
      return Set.of();
    });

    try (ConfigManager manager = ConfigManager.builder().mapper(mapper).build()) {
      verify(factory, never()).close();
      assertSame(config, manager.load(file, TestConfig.class));
      manager.validate(config);
      verify(validator, times(2)).validate(config);
      verify(factory, never()).close();
    }
    verify(factory).close();
  }

  @Test
  void closeIsIdempotent() {
    try (ConfigManager manager = ConfigManager.builder().build()) {
      manager.close();
      manager.close();
    }
    verify(factory).close();
  }

  @Test
  void failedValidatorCreationClosesFactory() {
    ValidationException failure = new ValidationException("getValidator failed");
    when(factory.getValidator()).thenThrow(failure);

    assertSame(failure, assertThrows(ValidationException.class,
        () -> ConfigManager.builder().build()));
    verify(factory).close();
  }

  @Test
  void failedManagerConstructionClosesFactory() {
    when(factory.getValidator()).thenReturn(null);

    assertThrows(NullPointerException.class, () -> ConfigManager.builder().build());
    verify(factory).close();
  }

  @Test
  void failedBuildPreservesFailureWhenCleanupAlsoFails() {
    ValidationException failure = new ValidationException("getValidator failed");
    when(factory.getValidator()).thenThrow(failure);
    ValidationException closeFailure = new ValidationException("close failed");
    doThrow(closeFailure).when(factory).close();

    assertSame(failure, assertThrows(ValidationException.class,
        () -> ConfigManager.builder().build()));
    assertEquals(1, failure.getSuppressed().length);
    assertSame(closeFailure, failure.getSuppressed()[0]);
    verify(factory).close();
  }

  @Test
  void closeFailureStillClosesManagerAndIsNotRetried() {
    ValidationException failure = new ValidationException("close failed");
    doThrow(failure).when(factory).close();

    try (ConfigManager manager = ConfigManager.builder().build()) {
      assertSame(failure, assertThrows(ValidationException.class, manager::close));
      assertDoesNotThrow(manager::close);
      assertThrows(IllegalStateException.class, () -> manager.validate(new TestConfig()));
    }
    verify(factory).close();
  }

  @Test
  void closedManagerRejectsLoadAndValidationBeforeUsingCollaborators() {
    try (ConfigManager manager = ConfigManager.builder().mapper(mapper).build()) {
      manager.close();

      IllegalStateException loadFailure = assertThrows(IllegalStateException.class,
          () -> manager.load(new File("unread.yml"), TestConfig.class));
      IllegalStateException validationFailure = assertThrows(IllegalStateException.class,
          () -> manager.validate(new TestConfig()));
      assertEquals("ConfigManager is closed", loadFailure.getMessage());
      assertEquals("ConfigManager is closed", validationFailure.getMessage());
      verifyNoInteractions(mapper, validator);
    }
    verify(factory).close();
  }

  @Test
  void suppliedValidatorRemainsUsableByCallerAndOtherManagers() {
    ValidatorFactory callerFactory = mock(ValidatorFactory.class);
    Validator sharedValidator = mock(Validator.class);
    when(callerFactory.getValidator()).thenReturn(sharedValidator);
    try (ConfigManager first = ConfigManager.builder()
        .validator(callerFactory.getValidator()).build();
        ConfigManager second = new ConfigManager(mapper, sharedValidator)) {
      first.close();
      second.validate(new TestConfig());
      assertTrue(sharedValidator.validate(new TestConfig()).isEmpty());
      verify(callerFactory, never()).close();
    }
    verify(callerFactory, never()).close();
    verifyNoInteractions(factory);
    validation.verifyNoInteractions();
  }
}
