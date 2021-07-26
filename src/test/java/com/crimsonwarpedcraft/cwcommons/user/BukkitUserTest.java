package com.crimsonwarpedcraft.cwcommons.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crimsonwarpedcraft.cwcommons.mock.MockPlayer;
import com.crimsonwarpedcraft.cwcommons.mock.MockPlayerData;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Paths;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for BukkitUser.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class BukkitUserTest {
  private static UserStore store;

  @BeforeAll
  static void setUp() {
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
  void sendMessage() {
    MockPlayer player = new MockPlayer();
    BukkitUser user = store.getUser(player);

    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> user.sendMessage(null)
    );

    // Check that messages are sent correctly
    user.sendMessage(Component.text("test"));
    assertEquals(Component.text("test"), player.getLastMessage());
  }


  @Test
  void getName() {
    MockPlayer player = new MockPlayer();
    player.setName("player1");
    assertEquals(
        "player1",
        store
            .getUser(player)
            .getName()
    );
  }
}