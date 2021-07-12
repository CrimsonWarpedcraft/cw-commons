package com.crimsonwarpedcraft.cwcommons.mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mock object for Plugins.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings({"NP_NONNULL_RETURN_VIOLATION", "EI_EXPOSE_REP"})
public class MockPlugin implements Plugin {
  private final Server server = new MockServer();

  @Override
  public @NotNull File getDataFolder() {
    return null;
  }

  @Override
  public @NotNull PluginDescriptionFile getDescription() {
    return null;
  }

  @Override
  public @NotNull FileConfiguration getConfig() {
    return null;
  }

  @Override
  public @Nullable InputStream getResource(@NotNull String filename) {
    return null;
  }

  @Override
  public void saveConfig() {

  }

  @Override
  public void saveDefaultConfig() {

  }

  @Override
  public void saveResource(@NotNull String resourcePath, boolean replace) {

  }

  @Override
  public void reloadConfig() {

  }

  @Override
  public @NotNull PluginLoader getPluginLoader() {
    return null;
  }

  @Override
  public @NotNull Server getServer() {
    return server;
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public void onDisable() {

  }

  @Override
  public void onLoad() {

  }

  @Override
  public void onEnable() {

  }

  @Override
  public boolean isNaggable() {
    return false;
  }

  @Override
  public void setNaggable(boolean canNag) {

  }

  @Override
  public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName,
                                                           @Nullable String id) {
    return null;
  }

  @Override
  public @NotNull Logger getLogger() {
    return null;
  }

  @Override
  public @NotNull String getName() {
    return null;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
                           @NotNull Command command,
                           @NotNull String label,
                           @NotNull String[] args) {
    return false;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                              @NotNull Command command,
                                              @NotNull String alias,
                                              @NotNull String[] args) {
    return null;
  }
}
