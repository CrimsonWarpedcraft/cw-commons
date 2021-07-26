package com.crimsonwarpedcraft.cwcommons.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.crimsonwarpedcraft.cwcommons.mock.MockPlayer;
import com.crimsonwarpedcraft.cwcommons.mock.MockPlayerData;
import com.crimsonwarpedcraft.cwcommons.mock.MockServer;
import com.crimsonwarpedcraft.cwcommons.user.BukkitPlayer;
import com.crimsonwarpedcraft.cwcommons.user.UserStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Paths;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for UserStoreLogoutMonitor.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class UserStoreLogoutMonitorTest {
  private static UserStore store;

  @BeforeAll
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  static void setUp() {
    if (Bukkit.getServer() == null) {
      Bukkit.setServer(new MockServer());
    }
    assertDoesNotThrow(
        () -> store = UserStore.getNewUserStore(
            Paths
                .get(
                    System.getProperty("java.io.tmpdir"),
                    "CwCommons" + System.currentTimeMillis(),
                    "user_data"
                )
                .toFile(),
            MockPlayerData.class,
            new MockPlayerData()
        )
    );
  }

  @Test
  void onPlayerQuit() {
    UserStoreLogoutMonitor monitor = UserStoreLogoutMonitor.getNewUserStoreLogoutMonitor(store);
    MockPlayer mockPlayer = new MockPlayer();

    // Store the player in the user store
    BukkitPlayer player = store.getUser(mockPlayer);

    // Run the event listener
    monitor.onPlayerQuit(
        new PlayerQuitEvent(
            mockPlayer,
            Component.empty(),
            PlayerQuitEvent.QuitReason.DISCONNECTED
        )
    );

    // Check to see that the player is no longer in the player store
    assertNotSame(
        player,
        store.getUser(mockPlayer)
    );
  }
}