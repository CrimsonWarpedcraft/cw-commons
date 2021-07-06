package com.crimsonwarpedcraft.cwcommons.command;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for PaperCommandRegister.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
class PaperCommandRegisterTest {
  @Test
  void getNewPaperCommandRegister() {
    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> PaperCommandRegister.getNewPaperCommandRegister(null)
    );
  }
}