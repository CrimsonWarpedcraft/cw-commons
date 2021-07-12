package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Tests for ConfigOption.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
class ConfigOptionTest {

  @Test
  void getNewConfigOption() {
    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> ConfigOption.getNewConfigOption(null, "")
    );
    assertThrows(
        NullPointerException.class,
        () -> ConfigOption.getNewConfigOption("", null)
    );
  }

  @Test
  void getName() {
    ConfigOption option = ConfigOption.getNewConfigOption("test1", "test2");
    assertEquals("test1", option.getName());
  }

  @Test
  void getValue() {
    ConfigOption option = ConfigOption.getNewConfigOption("test1", "test2");
    assertEquals("test2", option.getDefaultValue());
  }

  @Test
  void getValidityChecker() {
    // Make sure we can't set a validity checker that doesn't match the value
    assertThrows(
        IllegalArgumentException.class,
        () -> ConfigOption.getNewConfigOption(
            "test1",
            "test2",
            o -> false
        )
    );

    // Check that we get the predicate back correctly
    Predicate<Object> predicate = o -> true;
    assertEquals(
        predicate,
        ConfigOption
            .getNewConfigOption(
                "test1",
                "test2",
                predicate
            )
            .getValidityChecker()
    );
  }
}