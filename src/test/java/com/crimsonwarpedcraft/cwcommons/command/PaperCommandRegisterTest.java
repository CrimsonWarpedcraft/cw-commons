package com.crimsonwarpedcraft.cwcommons.command;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crimsonwarpedcraft.cwcommons.mock.MockPlugin;
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

  @Test
  void register() {
    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> PaperCommandRegister.getNewPaperCommandRegister(new MockPlugin()).register(null)
    );
  }
}