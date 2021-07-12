package com.crimsonwarpedcraft.cwcommons.user;

import io.papermc.lib.PaperLib;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Represents a BukkitUser based on an online Player.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class BukkitPlayer<T extends PlayerData> extends BukkitUser {
  private final Player player;
  private final T data;

  protected BukkitPlayer(Player player, T data) {
    super(Objects.requireNonNull(player));
    this.player = player;
    this.data = Objects.requireNonNull(data);
  }

  /** Sets the player's game mode. */
  public BukkitPlayer<T> setGameMode(GameMode gameMode) {
    player.setGameMode(Objects.requireNonNull(gameMode));

    return this;
  }

  /** Gets the player's game mode. */
  public GameMode getGameMode() {
    return player.getGameMode();
  }

  /** Gets the player's location. */
  public Location getLocation() {
    return player.getLocation();
  }

  /**
   * Teleports a player to the location (asynchronously if possible).
   *
   * @param location the location to teleport the player to
   * @return a CompletableFuture indicating whether the teleportation succeeded
   */
  public CompletableFuture<Boolean> teleport(Location location) {
    return PaperLib.teleportAsync(player, Objects.requireNonNull(location));
  }

  /** Returns the player's UUID. */
  public UUID getUuid() {
    return player.getUniqueId();
  }

  /** Returns the PlayerData for this player. */
  public T getPlayerData() {
    return data;
  }
}
