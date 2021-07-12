package com.crimsonwarpedcraft.cwcommons.mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mock object for PluginManager.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings({"NP_NONNULL_RETURN_VIOLATION", "EI_EXPOSE_REP"})
public class MockPluginManager implements PluginManager {
  private final Set<Listener> listeners = new HashSet<>();
  private Result result = Result.DEFAULT;
  private Event event;

  /** Represents possible outcomes for cancellable events. */
  public enum Result {
    TRUE,
    FALSE,
    DEFAULT
  }

  @Override
  public void registerInterface(@NotNull Class<? extends PluginLoader> loader)
      throws IllegalArgumentException {

  }

  @Override
  public @Nullable Plugin getPlugin(@NotNull String name) {
    return null;
  }

  @Override
  public @NotNull Plugin[] getPlugins() {
    return new Plugin[0];
  }

  @Override
  public boolean isPluginEnabled(@NotNull String name) {
    return false;
  }

  @Override
  public boolean isPluginEnabled(@Nullable Plugin plugin) {
    return false;
  }

  @Override
  public @Nullable Plugin loadPlugin(@NotNull File file)
      throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
    return null;
  }

  @Override
  public @NotNull Plugin[] loadPlugins(@NotNull File directory) {
    return new Plugin[0];
  }

  @Override
  public void disablePlugins() {

  }

  @Override
  public void clearPlugins() {

  }

  @Override
  public void callEvent(@NotNull Event event) throws IllegalStateException {
    this.event = event;

    if (result != Result.DEFAULT && event instanceof Cancellable) {
      ((Cancellable) event).setCancelled(result == Result.TRUE);
    }
  }

  public Event getLastEvent() {
    return event;
  }

  public void setCancellableResult(Result result) {
    this.result = result;
  }

  @Override
  public void registerEvents(@NotNull Listener listener, @NotNull Plugin plugin) {
    listeners.add(listener);
  }

  @Override
  public void registerEvent(@NotNull Class<? extends Event> event,
                            @NotNull Listener listener,
                            @NotNull EventPriority priority,
                            @NotNull EventExecutor executor,
                            @NotNull Plugin plugin) {

  }

  @Override
  public void registerEvent(@NotNull Class<? extends Event> event,
                            @NotNull Listener listener,
                            @NotNull EventPriority priority,
                            @NotNull EventExecutor executor,
                            @NotNull Plugin plugin,
                            boolean ignoreCancelled) {

  }

  @Override
  public void enablePlugin(@NotNull Plugin plugin) {

  }

  @Override
  public void disablePlugin(@NotNull Plugin plugin) {

  }

  @Override
  public void disablePlugin(@NotNull Plugin plugin, boolean closeClassloader) {

  }

  @Override
  public @Nullable Permission getPermission(@NotNull String name) {
    return null;
  }

  @Override
  public void addPermission(@NotNull Permission perm) {

  }

  @Override
  public void removePermission(@NotNull Permission perm) {

  }

  @Override
  public void removePermission(@NotNull String name) {

  }

  @Override
  public @NotNull Set<Permission> getDefaultPermissions(boolean op) {
    return null;
  }

  @Override
  public void recalculatePermissionDefaults(@NotNull Permission perm) {

  }

  @Override
  public void subscribeToPermission(@NotNull String permission,
                                    @NotNull Permissible permissible) {

  }

  @Override
  public void unsubscribeFromPermission(@NotNull String permission,
                                        @NotNull Permissible permissible) {

  }

  @Override
  public @NotNull Set<Permissible> getPermissionSubscriptions(@NotNull String permission) {
    return null;
  }

  @Override
  public void subscribeToDefaultPerms(boolean op, @NotNull Permissible permissible) {

  }

  @Override
  public void unsubscribeFromDefaultPerms(boolean op, @NotNull Permissible permissible) {

  }

  @Override
  public @NotNull Set<Permissible> getDefaultPermSubscriptions(boolean op) {
    return null;
  }

  @Override
  public @NotNull Set<Permission> getPermissions() {
    return null;
  }

  @Override
  public boolean useTimings() {
    return false;
  }

  public Set<Listener> getListeners() {
    return listeners;
  }
}
