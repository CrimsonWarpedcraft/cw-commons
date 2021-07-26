package com.crimsonwarpedcraft.cwcommons.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crimsonwarpedcraft.cwcommons.mock.MockPlayer;
import com.crimsonwarpedcraft.cwcommons.mock.MockPlayerData;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.Paths;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for UserStore.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class UserStoreTest {
  private static UserStore store;

  @BeforeAll
  static void setUp() {
    File dataFile = Paths
        .get(
            System.getProperty("java.io.tmpdir"),
            "CwCommons" + System.currentTimeMillis(),
            "user_data"
        )
        .toFile();

    assertDoesNotThrow(
        () -> store = UserStore.getNewUserStore(
            dataFile,
            MockPlayerData.class,
            new MockPlayerData()
        )
    );
  }

  @Test
  void getUser() {
    Player mockPlayer = new MockPlayer();

    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> store.getUser((CommandSender) null)
    );
    assertThrows(
        NullPointerException.class,
        () -> store.getUser(null)
    );

    // Get a BukkitPlayer instance
    BukkitPlayer player = store.getUser(mockPlayer);

    // Make sure that we get the same instance after each call
    assertSame(
        player,
        store.getUser(mockPlayer)
    );

    // Make sure that we don't get the same instance if we convert player to a CommandSender
    assertNotSame(
        player,
        store.getUser((CommandSender) mockPlayer)
    );
  }

  @Test
  void unloadPlayer() {
    Player mockPlayer = new MockPlayer();

    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> store.unloadPlayer(null)
    );


    // Get a BukkitPlayer instance
    BukkitPlayer player = store.getUser(mockPlayer);

    // Modify the player's data
    MockPlayerData
        .of(
            player.getPlayerData()
        )
        .setRandomVal(5);

    // Delete the instance from the store
    assertDoesNotThrow(
        () -> store.unloadPlayer(mockPlayer)
    );

    // Make sure we don't get the same object anymore
    assertNotSame(
        player,
        store.getUser(mockPlayer)
    );

    // Make sure we get the stored value back
    assertEquals(
        5,
        MockPlayerData
            .of(
                store
                    .getUser(mockPlayer)
                    .getPlayerData()
            )
            .getRandomVal()
    );
  }

  @Test
  void getNewUserStore() {
    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> UserStore.getNewUserStore(
            null,
            MockPlayerData.class,
            new MockPlayerData()
        )
    );
    assertThrows(
        NullPointerException.class,
        () -> UserStore
            .getNewUserStore(
                Paths
                    .get(
                        System.getProperty("java.io.tmpdir"),
                        "CwCommons" + System.currentTimeMillis(),
                        "user_data"
                    )
                    .toFile(),
                null,
                new MockPlayerData()
            )
    );
    assertThrows(
        NullPointerException.class,
        () -> UserStore
            .getNewUserStore(
                Paths
                    .get(
                        System.getProperty("java.io.tmpdir"),
                        "CwCommons" + System.currentTimeMillis(),
                        "user_data"
                    )
                    .toFile(),
                MockPlayerData.class,
                null
            )
    );
  }
}