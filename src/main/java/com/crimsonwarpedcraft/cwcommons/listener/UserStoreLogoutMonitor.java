package com.crimsonwarpedcraft.cwcommons.listener;

import com.crimsonwarpedcraft.cwcommons.user.UserStore;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Removes players from the UserStore when they are no longer needed.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class UserStoreLogoutMonitor implements Listener {
  private static final Logger LOGGER = Bukkit.getLogger();
  private final UserStore store;

  public static UserStoreLogoutMonitor getNewUserStoreLogoutMonitor(UserStore store) {
    return new UserStoreLogoutMonitor(store);
  }

  private UserStoreLogoutMonitor(UserStore store) {
    this.store = Objects.requireNonNull(store);
  }

  /** Removes players from the user store when players leave the game. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    // Remove player from user store
    try {
      store.unloadPlayer(event.getPlayer());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error saving player data", e);
    }
  }
}
