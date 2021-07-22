package com.crimsonwarpedcraft.cwcommons.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crimsonwarpedcraft.cwcommons.mock.MockListener;
import com.crimsonwarpedcraft.cwcommons.mock.MockPlugin;
import com.crimsonwarpedcraft.cwcommons.mock.MockPluginManager;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.Test;

/**
 * Tests for BukkitListenerRegister.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
class BukkitListenerRegisterTest {

  @Test
  void registerListener() {
    MockPlugin plugin = new MockPlugin();
    BukkitListenerRegister register = BukkitListenerRegister.getNewBukkitListenerRegister(plugin);

    // Check NPE
    assertThrows(NullPointerException.class, () -> register.registerListener(null));

    // Register a mock listener with the register
    Listener listener = new MockListener();
    register.registerListener(listener);

    // Check that the listener was registered successfully
    assertTrue(
        ((MockPluginManager) plugin
            .getServer()
            .getPluginManager())
            .getListeners()
            .contains(listener)
    );
  }

  @Test
  void getListenerRegister() {
    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> BukkitListenerRegister.getNewBukkitListenerRegister(null)
    );
  }
}