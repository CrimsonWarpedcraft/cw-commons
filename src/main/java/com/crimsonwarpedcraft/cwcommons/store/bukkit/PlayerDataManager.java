package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.crimsonwarpedcraft.cwcommons.store.Repository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 * Convenience wrapper around a {@link Repository} that uses {@link UUID} keys and flushes
 * pending writes when a player disconnects.
 *
 * <p>Register an instance in {@code onEnable()} and use {@link #get}/{@link #save} in place of
 * the raw repository. All pending writes are flushed to the backend on each
 * {@link PlayerQuitEvent}.
 *
 * @param <T> the per-player data type
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class PlayerDataManager<T> implements Listener {

  private final Repository<UUID, T> repository;
  private final Plugin plugin;

  /**
   * Creates a {@link PlayerDataManager} backed by the given repository and registers it as a
   * Bukkit event listener.
   *
   * @param repository the repository to wrap; must use {@link UUID} keys
   * @param plugin the owning plugin, used for listener registration
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
      justification = "Repository is an interface; defensive copy is not meaningful here")
  public PlayerDataManager(Repository<UUID, T> repository, Plugin plugin) {
    this.repository = Objects.requireNonNull(repository);
    this.plugin = Objects.requireNonNull(plugin);
  }

  /**
   * Returns the stored data for the given player, or empty if none exists yet.
   *
   * @param player the player to look up
   * @return a future completing with the player's data, or empty
   */
  public CompletableFuture<Optional<T>> get(Player player) {
    return repository.get(Objects.requireNonNull(player).getUniqueId());
  }

  /**
   * Stores the given data for the given player.
   *
   * @param player the player to store data for
   * @param data the data to store
   * @return a future completing when the write is acknowledged
   */
  public CompletableFuture<Void> save(Player player, T data) {
    return repository.put(
        Objects.requireNonNull(player).getUniqueId(), Objects.requireNonNull(data));
  }

  /**
   * Flushes all pending player data writes to the backend when a player disconnects.
   *
   * @param event the quit event
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    repository.flush();
  }

  /**
   * Registers this manager as a Bukkit event listener with the owning plugin.
   *
   * <p>Call this once in {@code onEnable()} after constructing the manager.
   */
  public void registerEvents() {
    this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
  }
}
