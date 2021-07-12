package com.crimsonwarpedcraft.cwcommons.user;

import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Used to get an instance of a BukkitUser from a Player or CommandSender.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class UserStore<T extends PlayerData> {
  private final Class<T> playerDataClass;
  private final File dataDirectory;
  private final Map<Player, BukkitPlayer<T>> players;
  private final T defaultPlayerData;

  /**
   * Returns a new UserStore instance which persists player data.
   *
   * @param dataDirectory the directory to save the PlayerData to
   * @param playerDataClass the PlayerData class to use for storing persistent data
   * @param defaultPlayerData a PlayerData object to be used as the default object
   */
  public static <T extends PlayerData> UserStore<T> getNewUserStore(
      File dataDirectory,
      Class<T> playerDataClass,
      T defaultPlayerData
  ) throws FileNotFoundException {
    Objects.requireNonNull(dataDirectory);

    if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
      throw new FileNotFoundException("User storage folder " + dataDirectory.getAbsolutePath()
          + " does not exist and could not be created");
    }

    return new UserStore<>(dataDirectory, playerDataClass, defaultPlayerData);
  }

  protected UserStore(
      File dataDirectory,
      Class<T> playerDataClass,
      T defaultPlayerData
  ) {
    players = new HashMap<>();
    this.dataDirectory = dataDirectory;
    this.playerDataClass = Objects.requireNonNull(playerDataClass);
    this.defaultPlayerData = Objects.requireNonNull(defaultPlayerData);
  }

  /**
   * Gets the BukkitPlayer for the Player.
   *
   * @param player the player to get the BukkitPlayer from
   * @return the existing BukkitPlayer for this Player if found, otherwise a new instance is
   *     returned
   */
  public BukkitPlayer<T> getUser(Player player) {
    Objects.requireNonNull(player);

    if (players.containsKey(player)) {
      return players.get(player);
    }

    BukkitPlayer<T> bukkitPlayer = new BukkitPlayer<>(player, loadPlayerData(player));

    players.put(player, bukkitPlayer);

    return bukkitPlayer;
  }

  /**
   * Gets the BukkitUser for the CommandSender.
   *
   * @param sender the command sender to get the BukkitUser from
   * @return a new BukkitUser
   */
  public BukkitUser getUser(CommandSender sender) {
    return new BukkitUser(Objects.requireNonNull(sender));
  }

  /** Removes an exising BukkitPlayer for the player. */
  // UUIDs are not specified by user input
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public UserStore<T> unloadPlayer(Player player) throws IOException {
    BukkitPlayer<T> removedPlayer = players.remove(Objects.requireNonNull(player));

    if (removedPlayer != null && dataDirectory != null) {
      File dataFile = new File(dataDirectory, removedPlayer.getUuid() + ".json");
      removedPlayer.getPlayerData().write(dataFile);
    }

    return this;
  }

  // UUIDs are not specified by user input
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  protected T loadPlayerData(Player player) {
    T data = null;
    // If we have data for this player, try to load it
    if (dataDirectory != null) {
      File dataFile = new File(dataDirectory, player.getUniqueId() + ".json");

      if (dataFile.exists()) {
        Gson gson = new Gson();
        try (
            Reader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(dataFile),
                    StandardCharsets.UTF_8
                )
            )
        ) {
          data = gson.fromJson(
              reader,
              playerDataClass
          );

        } catch (IOException e) {
          data = null;
        }
      }
    }

    // If the data didn't get loaded and we have a default, use a copy of that
    PlayerData temp = defaultPlayerData.copy();
    if (data == null) {
      data = (T) temp;
    }

    return data;
  }
}
