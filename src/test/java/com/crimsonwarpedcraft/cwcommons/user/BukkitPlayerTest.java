package com.crimsonwarpedcraft.cwcommons.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crimsonwarpedcraft.cwcommons.mock.MockPlayer;
import com.crimsonwarpedcraft.cwcommons.mock.MockPlayerData;
import com.crimsonwarpedcraft.cwcommons.mock.MockServer;
import com.crimsonwarpedcraft.cwcommons.mock.MockWorld;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Paths;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for BukkitPlayer.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class BukkitPlayerTest {
  private static UserStore<MockPlayerData> store;

  @BeforeAll
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  static void setUp() {
    if (Bukkit.getServer() == null) {
      Bukkit.setServer(new MockServer());
    }

    MockPlayerData data = new MockPlayerData();
    data.setRandomVal(5);
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
            data
        )
    );
  }

  @Test
  void setGameMode() {
    MockPlayer player = new MockPlayer();
    player.setGameMode(GameMode.ADVENTURE);

    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> store
            .getUser(player)
            .setGameMode(null)
    );

    // Check that the player's gamemode was set
    store
        .getUser(player)
        .setGameMode(GameMode.SURVIVAL);
    assertEquals(GameMode.SURVIVAL, player.getGameMode());
  }

  @Test
  void getUuid() {
    MockPlayer player = new MockPlayer();

    assertEquals(
        player.getUniqueId(),
        store
            .getUser(player)
            .getUuid()
    );
  }

  @Test
  void getGameMode() {
    MockPlayer player = new MockPlayer();
    player.setGameMode(GameMode.SURVIVAL);
    assertEquals(GameMode.SURVIVAL, store.getUser(player).getGameMode());
  }

  @Test
  void getLocation() {
    MockPlayer player = new MockPlayer();
    Location location = new Location(new MockWorld(), 0, 0, 0);
    player.teleport(location);

    // Make sure the location is retrieved properly
    assertEquals(
        location,
        store
            .getUser(player)
            .getLocation()
    );
  }

  @Test
  void teleport() {
    MockPlayer player = new MockPlayer();

    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> store
            .getUser(player)
            .setGameMode(null)
    );

    // Make sure that teleportation works
    Location location = new Location(new MockWorld(), 0, 0, 0);
    store
        .getUser(player)
        .teleport(location);
    assertEquals(location, player.getLocation());
  }

  @Test
  void getPlayerData() {
    MockPlayer player = new MockPlayer();
    assertEquals(
        5,
        store
            .getUser(player)
            .getPlayerData()
            .getRandomVal()
    );
  }
}