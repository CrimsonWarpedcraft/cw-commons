package com.crimsonwarpedcraft.cwcommons.command;

import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;

/**
 * Tests for CommandRegister.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_INFERRED",
    justification = "We don't need the return value"
)
class CommandRegisterTest {
  @Test
  void register_throws_null_pointer_exception_on_null_input() {
    assertThrows(
        NullPointerException.class,
        () -> CommandRegister.getNewCommandRegister().register(null)
    );
  }

  @Test
  void unregister_throws_null_pointer_exception_on_null_input() {
    assertThrows(
        NullPointerException.class,
        () -> CommandRegister.getNewCommandRegister().unregister(null)
    );
  }
}