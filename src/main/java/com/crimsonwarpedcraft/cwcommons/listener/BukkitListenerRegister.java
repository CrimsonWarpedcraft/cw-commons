package com.crimsonwarpedcraft.cwcommons.listener;

import java.util.Objects;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 * Registers event listeners with the server.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class BukkitListenerRegister {
  private final Plugin plugin;
  private final PluginManager manager;

  private BukkitListenerRegister(Plugin plugin) {
    this.plugin = Objects.requireNonNull(plugin);
    this.manager = Objects.requireNonNull(plugin).getServer().getPluginManager();
  }

  /**
   * Registers an event listener with the server.
   *
   * @param listener the event listener to register
   * @return this instance that can be used for method chaining
   */
  public BukkitListenerRegister registerListener(Listener listener) {
    manager.registerEvents(Objects.requireNonNull(listener), plugin);

    return this;
  }

  /**
   * Constructs and returns a ListenerRegister object.
   *
   * @param plugin the plugin that is registering the event listeners
   * @return a ListenerRegister object
   */
  public static BukkitListenerRegister getNewBukkitListenerRegister(Plugin plugin) {
    return new BukkitListenerRegister(plugin);
  }
}
