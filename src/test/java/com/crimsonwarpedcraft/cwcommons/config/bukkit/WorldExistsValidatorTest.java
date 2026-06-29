package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldExistsValidatorTest {

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

  record AnnotatedHolder(@WorldExists Location loc) {}

  record PlainHolder(Location loc) {}

  private static Location locationWithWorld(World world) {
    Location loc = mock(Location.class);
    when(loc.getWorld()).thenReturn(world);
    return loc;
  }

  @Test
  void loadedWorldPasses() {
    Location loc = locationWithWorld(mock(World.class));
    assertEquals(0, validator.validate(new AnnotatedHolder(loc)).size());
  }

  @Test
  void unloadedWorldFails() {
    Location loc = locationWithWorld(null);
    assertEquals(1, validator.validate(new AnnotatedHolder(loc)).size());
  }

  @Test
  void nullLocationPasses() {
    assertEquals(0, validator.validate(new AnnotatedHolder(null)).size());
  }

  @Test
  void unannotatedFieldIsNotChecked() {
    Location loc = locationWithWorld(null);
    assertEquals(0, validator.validate(new PlainHolder(loc)).size());
  }
}
