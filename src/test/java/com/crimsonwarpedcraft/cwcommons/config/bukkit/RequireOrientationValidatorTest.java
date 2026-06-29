package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.bukkit.Location;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequireOrientationValidatorTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  record AnnotatedHolder(@RequireOrientation Location loc) {}

  record PlainHolder(Location loc) {}

  private static Location locationWithOrientation(float yaw, float pitch) {
    Location loc = mock(Location.class);
    when(loc.getYaw()).thenReturn(yaw);
    when(loc.getPitch()).thenReturn(pitch);
    return loc;
  }

  @Test
  void presentOrientationPasses() {
    Location loc = locationWithOrientation(90.0f, -30.0f);
    assertEquals(0, validator.validate(new AnnotatedHolder(loc)).size());
  }

  @Test
  void missingYawFails() {
    Location loc = locationWithOrientation(Float.NaN, -30.0f);
    assertEquals(1, validator.validate(new AnnotatedHolder(loc)).size());
  }

  @Test
  void missingPitchFails() {
    Location loc = locationWithOrientation(90.0f, Float.NaN);
    assertEquals(1, validator.validate(new AnnotatedHolder(loc)).size());
  }

  @Test
  void nullLocationPasses() {
    assertEquals(0, validator.validate(new AnnotatedHolder(null)).size());
  }

  @Test
  void unannotatedFieldIsNotChecked() {
    Location loc = locationWithOrientation(Float.NaN, Float.NaN);
    assertEquals(0, validator.validate(new PlainHolder(loc)).size());
  }
}
