package com.crimsonwarpedcraft.cwcommons.mock;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.profile.PlayerProfile;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.papermc.paper.datapack.DatapackManager;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.StructureType;
import org.bukkit.Tag;
import org.bukkit.UnsafeValues;
import org.bukkit.Warning;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.help.HelpMap;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.Recipe;
import org.bukkit.loot.LootTable;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.potion.PotionBrewer;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.CachedServerIcon;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mock object for Server.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings({"NP_NONNULL_RETURN_VIOLATION", "EI_EXPOSE_REP"})
public class MockServer implements Server {
  private final HashSet<Player> players = new HashSet<>();
  private final HashMap<String, World> worlds = new HashMap<>();
  private final List<Recipe> recipes = new LinkedList<>();
  private final PluginManager manager = new MockPluginManager();

  @Override
  public @NotNull File getPluginsFolder() {
    return null;
  }

  @Override
  public @NotNull String getName() {
    return null;
  }

  @Override
  public @NotNull String getVersion() {
    return "(MC: 1.16.5)";
  }

  @Override
  public @NotNull String getBukkitVersion() {
    return null;
  }

  @Override
  public @NotNull String getMinecraftVersion() {
    return null;
  }

  @Override
  public @NotNull Collection<? extends Player> getOnlinePlayers() {
    return players;
  }

  public void addPlayer(Player player) {
    players.add(player);
  }

  @Override
  public int getMaxPlayers() {
    return 0;
  }

  @Override
  public void setMaxPlayers(int maxPlayers) {

  }

  @Override
  public int getPort() {
    return 0;
  }

  @Override
  public int getViewDistance() {
    return 0;
  }

  @Override
  public int getSimulationDistance() {
    return 0;
  }

  @Override
  public @NotNull String getIp() {
    return null;
  }

  @Override
  public @NotNull String getWorldType() {
    return null;
  }

  @Override
  public boolean getGenerateStructures() {
    return false;
  }

  @Override
  public int getMaxWorldSize() {
    return 0;
  }

  @Override
  public boolean getAllowEnd() {
    return false;
  }

  @Override
  public boolean getAllowNether() {
    return false;
  }

  @Override
  public @NotNull String getResourcePack() {
    return null;
  }

  @Override
  public @NotNull String getResourcePackHash() {
    return null;
  }

  @Override
  public @NotNull String getResourcePackPrompt() {
    return null;
  }

  @Override
  public boolean isResourcePackRequired() {
    return false;
  }

  @Override
  public boolean hasWhitelist() {
    return false;
  }

  @Override
  public void setWhitelist(boolean value) {

  }

  @Override
  public boolean isWhitelistEnforced() {
    return false;
  }

  @Override
  public void setWhitelistEnforced(boolean value) {

  }

  @Override
  public @NotNull Set<OfflinePlayer> getWhitelistedPlayers() {
    return null;
  }

  @Override
  public void reloadWhitelist() {

  }

  @Override
  public int broadcastMessage(@NotNull String message) {
    return 0;
  }

  @Override
  public @NotNull String getUpdateFolder() {
    return null;
  }

  @Override
  public @NotNull File getUpdateFolderFile() {
    return null;
  }

  @Override
  public long getConnectionThrottle() {
    return 0;
  }

  @Override
  public int getTicksPerAnimalSpawns() {
    return 0;
  }

  @Override
  public int getTicksPerMonsterSpawns() {
    return 0;
  }

  @Override
  public int getTicksPerWaterSpawns() {
    return 0;
  }

  @Override
  public int getTicksPerWaterAmbientSpawns() {
    return 0;
  }

  @Override
  public int getTicksPerWaterUndergroundCreatureSpawns() {
    return 0;
  }

  @Override
  public int getTicksPerAmbientSpawns() {
    return 0;
  }

  @Override
  public int getTicksPerSpawns(@NotNull SpawnCategory spawnCategory) {
    return 0;
  }

  @Override
  public @Nullable Player getPlayer(@NotNull String name) {
    return null;
  }

  @Override
  public @Nullable Player getPlayer(@NotNull UUID id) {
    return null;
  }

  @Override
  public @Nullable Player getPlayerExact(@NotNull String name) {
    return null;
  }

  @Override
  public @NotNull List<Player> matchPlayer(@NotNull String name) {
    return null;
  }

  @Override
  public @Nullable UUID getPlayerUniqueId(@NotNull String playerName) {
    return null;
  }

  @Override
  public @NotNull PluginManager getPluginManager() {
    return manager;
  }

  @Override
  public @NotNull BukkitScheduler getScheduler() {
    return null;
  }

  @Override
  public @NotNull ServicesManager getServicesManager() {
    return null;
  }

  @Override
  public @NotNull List<World> getWorlds() {
    return null;
  }

  @Override
  public @Nullable World createWorld(@NotNull WorldCreator creator) {
    return null;
  }

  @Override
  public boolean unloadWorld(@NotNull String name, boolean save) {
    return false;
  }

  @Override
  public boolean unloadWorld(@NotNull World world, boolean save) {
    return false;
  }

  public void loadWorld(World world) {
    worlds.put(world.getName(), world);
  }

  @Override
  public @Nullable World getWorld(@NotNull String name) {
    return worlds.get(name);
  }

  @Override
  public @Nullable World getWorld(@NotNull UUID uid) {
    return null;
  }

  @Override
  public @Nullable World getWorld(@NotNull NamespacedKey worldKey) {
    return null;
  }

  @Override
  public @Nullable MapView getMap(int id) {
    return null;
  }

  @Override
  public @NotNull MapView createMap(@NotNull World world) {
    return null;
  }

  @Override
  public @NotNull ItemStack createExplorerMap(@NotNull World world,
                                              @NotNull Location location,
                                              @NotNull StructureType structureType) {
    return null;
  }

  @Override
  public @NotNull ItemStack createExplorerMap(@NotNull World world,
                                              @NotNull Location location,
                                              @NotNull StructureType structureType,
                                              int radius,
                                              boolean findUnexplored) {
    return null;
  }

  @Override
  public void reload() {

  }

  @Override
  public void reloadData() {

  }

  @Override
  public @NotNull Logger getLogger() {
    return Logger.getLogger(this.getClass().getName());
  }

  @Override
  public @Nullable PluginCommand getPluginCommand(@NotNull String name) {
    return null;
  }

  @Override
  public void savePlayers() {

  }

  @Override
  public boolean dispatchCommand(@NotNull CommandSender sender,
                                 @NotNull String commandLine) throws CommandException {
    return false;
  }

  @Override
  public boolean addRecipe(@Nullable Recipe recipe) {
    return recipe != null && recipes.add(recipe);
  }

  @Override
  public @NotNull List<Recipe> getRecipesFor(@NotNull ItemStack result) {
    return null;
  }

  @Override
  public @Nullable Recipe getRecipe(@NotNull NamespacedKey recipeKey) {
    return null;
  }

  @Override
  public @Nullable Recipe getCraftingRecipe(
      @NotNull ItemStack[] craftingMatrix,
      @NotNull World world
  ) {
    return null;
  }

  @Override
  public @NotNull ItemStack craftItem(
      @NotNull ItemStack[] craftingMatrix,
      @NotNull World world,
      @NotNull Player player
  ) {
    return null;
  }

  @Override
  public @NotNull Iterator<Recipe> recipeIterator() {
    // Return a copy of recipes so it's effectively immutable
    return new LinkedList<>(recipes).listIterator();
  }

  public boolean hasRecipe(Recipe recipe) {
    return recipes.contains(recipe);
  }

  @Override
  public void clearRecipes() {
    recipes.clear();
  }

  @Override
  public void resetRecipes() {

  }

  @Override
  public boolean removeRecipe(@NotNull NamespacedKey key) {
    return false;
  }

  @Override
  public @NotNull Map<String, String[]> getCommandAliases() {
    return null;
  }

  @Override
  public int getSpawnRadius() {
    return 0;
  }

  @Override
  public void setSpawnRadius(int value) {

  }

  @Override
  public boolean getHideOnlinePlayers() {
    return false;
  }

  @Override
  public boolean getOnlineMode() {
    return false;
  }

  @Override
  public boolean getAllowFlight() {
    return false;
  }

  @Override
  public boolean isHardcore() {
    return false;
  }

  @Override
  public void shutdown() {

  }

  @Override
  public int broadcast(@NotNull String message, @NotNull String permission) {
    return 0;
  }

  @Override
  public int broadcast(@NotNull Component message) {
    return 0;
  }

  @Override
  public int broadcast(@NotNull Component message, @NotNull String permission) {
    return 0;
  }

  @Override
  public @NotNull OfflinePlayer getOfflinePlayer(@NotNull String name) {
    return null;
  }

  @Override
  public @NotNull OfflinePlayer getOfflinePlayer(@NotNull UUID id) {
    return null;
  }

  @Override
  public org.bukkit.profile.@NotNull PlayerProfile createPlayerProfile(
      @Nullable UUID uniqueId,
      @Nullable String name
  ) {
    return null;
  }

  @Override
  public org.bukkit.profile.@NotNull PlayerProfile createPlayerProfile(@NotNull UUID uniqueId) {
    return null;
  }

  @Override
  public org.bukkit.profile.@NotNull PlayerProfile createPlayerProfile(@NotNull String name) {
    return null;
  }

  @Override
  public @Nullable OfflinePlayer getOfflinePlayerIfCached(@NotNull String name) {
    return null;
  }

  @Override
  public @NotNull Set<String> getIPBans() {
    return null;
  }

  @Override
  public void banIP(@NotNull String address) {

  }

  @Override
  public void unbanIP(@NotNull String address) {

  }

  @Override
  public @NotNull Set<OfflinePlayer> getBannedPlayers() {
    return null;
  }

  @Override
  public @NotNull BanList getBanList(BanList.@NotNull Type type) {
    return null;
  }

  @Override
  public @NotNull Set<OfflinePlayer> getOperators() {
    return null;
  }

  @Override
  public @NotNull GameMode getDefaultGameMode() {
    return null;
  }

  @Override
  public void setDefaultGameMode(@NotNull GameMode mode) {

  }

  @Override
  public @NotNull ConsoleCommandSender getConsoleSender() {
    return null;
  }

  @Override
  public @NotNull CommandSender createCommandSender(@NotNull Consumer<? super Component> feedback) {
    return null;
  }

  @Override
  public @NotNull File getWorldContainer() {
    return null;
  }

  @Override
  public @NotNull OfflinePlayer[] getOfflinePlayers() {
    return new OfflinePlayer[0];
  }

  @Override
  public @NotNull Messenger getMessenger() {
    return null;
  }

  @Override
  public @NotNull HelpMap getHelpMap() {
    return null;
  }

  @Override
  public @NotNull Inventory createInventory(@Nullable InventoryHolder owner,
                                            @NotNull InventoryType type) {
    return null;
  }

  @Override
  public @NotNull Inventory createInventory(@Nullable InventoryHolder owner,
                                            @NotNull InventoryType type,
                                            @NotNull Component title) {
    return null;
  }

  @Override
  public @NotNull Inventory createInventory(@Nullable InventoryHolder owner,
                                            @NotNull InventoryType type,
                                            @NotNull String title) {
    return null;
  }

  @Override
  public @NotNull Inventory createInventory(@Nullable InventoryHolder owner, int size)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull Inventory createInventory(@Nullable InventoryHolder owner,
                                            int size,
                                            @NotNull Component title)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull Inventory createInventory(@Nullable InventoryHolder owner,
                                            int size,
                                            @NotNull String title) throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull Merchant createMerchant(@Nullable Component title) {
    return null;
  }

  @Override
  public @NotNull Merchant createMerchant(@Nullable String title) {
    return null;
  }

  @Override
  public int getMonsterSpawnLimit() {
    return 0;
  }

  @Override
  public int getAnimalSpawnLimit() {
    return 0;
  }

  @Override
  public int getWaterAnimalSpawnLimit() {
    return 0;
  }

  @Override
  public int getWaterAmbientSpawnLimit() {
    return 0;
  }

  @Override
  public int getWaterUndergroundCreatureSpawnLimit() {
    return 0;
  }

  @Override
  public int getAmbientSpawnLimit() {
    return 0;
  }

  @Override
  public int getSpawnLimit(@NotNull SpawnCategory spawnCategory) {
    return 0;
  }

  @Override
  public boolean isPrimaryThread() {
    return false;
  }

  @Override
  public @NotNull Component motd() {
    return null;
  }

  @Override
  public @NotNull String getMotd() {
    return null;
  }

  @Override
  public @Nullable Component shutdownMessage() {
    return null;
  }

  @Override
  public @Nullable String getShutdownMessage() {
    return null;
  }

  @Override
  public Warning.@NotNull WarningState getWarningState() {
    return null;
  }

  @Override
  public @NotNull ItemFactory getItemFactory() {
    return null;
  }

  @Override
  public @NotNull ScoreboardManager getScoreboardManager() {
    return null;
  }

  @Override
  public @Nullable CachedServerIcon getServerIcon() {
    return null;
  }

  @Override
  public @NotNull CachedServerIcon loadServerIcon(@NotNull File file)
      throws IllegalArgumentException, Exception {
    return null;
  }

  @Override
  public @NotNull CachedServerIcon loadServerIcon(@NotNull BufferedImage image)
      throws IllegalArgumentException, Exception {
    return null;
  }

  @Override
  public void setIdleTimeout(int threshold) {

  }

  @Override
  public int getIdleTimeout() {
    return 0;
  }

  @Override
  public ChunkGenerator.@NotNull ChunkData createChunkData(@NotNull World world) {
    return null;
  }

  @Override
  public ChunkGenerator.@NotNull ChunkData createVanillaChunkData(@NotNull World world,
                                                                  int x,
                                                                  int z) {
    return null;
  }

  @Override
  public @NotNull BossBar createBossBar(@Nullable String title,
                                        @NotNull BarColor color,
                                        @NotNull BarStyle style,
                                        @NotNull BarFlag... flags) {
    return null;
  }

  @Override
  public @NotNull KeyedBossBar createBossBar(@NotNull NamespacedKey key,
                                             @Nullable String title,
                                             @NotNull BarColor color,
                                             @NotNull BarStyle style,
                                             @NotNull BarFlag... flags) {
    return null;
  }

  @Override
  public @NotNull Iterator<KeyedBossBar> getBossBars() {
    return null;
  }

  @Override
  public @Nullable KeyedBossBar getBossBar(@NotNull NamespacedKey key) {
    return null;
  }

  @Override
  public boolean removeBossBar(@NotNull NamespacedKey key) {
    return false;
  }

  @Override
  public @Nullable Entity getEntity(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public @NotNull double[] getTPS() {
    return new double[0];
  }

  @Override
  public @NotNull long[] getTickTimes() {
    return new long[0];
  }

  @Override
  public double getAverageTickTime() {
    return 0;
  }

  @Override
  public @NotNull CommandMap getCommandMap() {
    return null;
  }

  @Override
  public @Nullable Advancement getAdvancement(@NotNull NamespacedKey key) {
    return null;
  }

  @Override
  public @NotNull Iterator<Advancement> advancementIterator() {
    return null;
  }

  @Override
  public @NotNull BlockData createBlockData(@NotNull Material material) {
    return null;
  }

  @Override
  public @NotNull BlockData createBlockData(@NotNull Material material,
                                            @Nullable Consumer<BlockData> consumer) {
    return null;
  }

  @Override
  public @NotNull BlockData createBlockData(@NotNull String data) throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull BlockData createBlockData(@Nullable Material material,
                                            @Nullable String data) throws IllegalArgumentException {
    return null;
  }

  @Override
  public <T extends Keyed> Tag<T> getTag(@NotNull String registry,
                                         @NotNull NamespacedKey tag,
                                         @NotNull Class<T> clazz) {
    return null;
  }

  @Override
  public @NotNull <T extends Keyed> Iterable<Tag<T>> getTags(@NotNull String registry,
                                                             @NotNull Class<T> clazz) {
    return null;
  }

  @Override
  public @Nullable LootTable getLootTable(@NotNull NamespacedKey key) {
    return null;
  }

  @Override
  public @NotNull List<Entity> selectEntities(@NotNull CommandSender sender,
                                              @NotNull String selector)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull StructureManager getStructureManager() {
    return null;
  }

  @Override
  public @NotNull UnsafeValues getUnsafe() {
    return null;
  }

  @Override
  public @NotNull Spigot spigot() {
    return null;
  }

  @Override
  public void reloadPermissions() {

  }

  @Override
  public boolean reloadCommandAliases() {
    return false;
  }

  @Override
  public boolean suggestPlayerNamesWhenNullTabCompletions() {
    return false;
  }

  @Override
  public @NotNull String getPermissionMessage() {
    return null;
  }

  @Override
  public @NotNull PlayerProfile createProfile(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public @NotNull PlayerProfile createProfile(@NotNull String name) {
    return null;
  }

  @Override
  public @NotNull PlayerProfile createProfile(@Nullable UUID uuid, @Nullable String name) {
    return null;
  }

  @Override
  public int getCurrentTick() {
    return 0;
  }

  @Override
  public boolean isStopping() {
    return false;
  }

  @Override
  public @NotNull MobGoals getMobGoals() {
    return null;
  }

  @Override
  public @NotNull DatapackManager getDatapackManager() {
    return null;
  }

  @Override
  public @NotNull PotionBrewer getPotionBrewer() {
    return null;
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return null;
  }

  @Override
  public void sendPluginMessage(@NotNull Plugin source,
                                @NotNull String channel,
                                @NotNull byte[] message) {

  }

  @Override
  public @NotNull Set<String> getListeningPluginChannels() {
    return null;
  }
}
