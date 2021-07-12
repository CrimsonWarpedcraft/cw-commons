package com.crimsonwarpedcraft.cwcommons.mock;

import com.destroystokyo.paper.HeightmapType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.papermc.paper.world.MoonPhase;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Difficulty;
import org.bukkit.Effect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameRule;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Raid;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.StructureType;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mock object for World.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
public class MockWorld implements World {
  private final WorldBorder border = new MockWorldBorder();
  private String name = "world";
  private Difficulty difficulty = Difficulty.NORMAL;
  private long time = 0;

  @Override
  public int getEntityCount() {
    return 0;
  }

  @Override
  public int getTileEntityCount() {
    return 0;
  }

  @Override
  public int getTickableTileEntityCount() {
    return 0;
  }

  @Override
  public int getChunkCount() {
    return 0;
  }

  @Override
  public int getPlayerCount() {
    return 0;
  }

  @Override
  public @NotNull MoonPhase getMoonPhase() {
    return null;
  }

  @Override
  public boolean lineOfSightExists(@NotNull Location from, @NotNull Location to) {
    return false;
  }

  @Override
  public @NotNull Block getBlockAt(int x, int y, int z) {
    return null;
  }

  @Override
  public @NotNull Block getBlockAt(@NotNull Location location) {
    return null;
  }

  @Override
  public int getHighestBlockYAt(int x, int z) {
    return 0;
  }

  @Override
  public int getHighestBlockYAt(@NotNull Location location) {
    return 0;
  }

  @Override
  public int getHighestBlockYAt(int x, int z, @NotNull HeightmapType heightmap)
      throws UnsupportedOperationException {
    return 0;
  }

  @Override
  public int getHighestBlockYAt(int x, int z, @NotNull HeightMap heightMap) {
    return 0;
  }

  @Override
  public int getHighestBlockYAt(@NotNull Location location, @NotNull HeightMap heightMap) {
    return 0;
  }

  @Override
  public @NotNull Block getHighestBlockAt(int x, int z) {
    return null;
  }

  @Override
  public @NotNull Block getHighestBlockAt(@NotNull Location location) {
    return null;
  }

  @Override
  public @NotNull Block getHighestBlockAt(int x, int z, @NotNull HeightMap heightMap) {
    return null;
  }

  @Override
  public @NotNull Block getHighestBlockAt(@NotNull Location location,
                                          @NotNull HeightMap heightMap) {
    return null;
  }

  @Override
  public @NotNull Chunk getChunkAt(int x, int z) {
    return null;
  }

  @Override
  public @NotNull Chunk getChunkAt(@NotNull Location location) {
    return null;
  }

  @Override
  public @NotNull Chunk getChunkAt(@NotNull Block block) {
    return null;
  }

  @Override
  public @NotNull CompletableFuture<Chunk> getChunkAtAsync(int x,
                                                           int z,
                                                           boolean gen,
                                                           boolean urgent) {
    return null;
  }

  @Override
  public @NotNull NamespacedKey getKey() {
    return null;
  }

  @Override
  public @NotNull Chunk[] getLoadedChunks() {
    return new Chunk[0];
  }

  @Override
  public boolean isChunkLoaded(@NotNull Chunk chunk) {
    return false;
  }

  @Override
  public boolean isChunkLoaded(int x, int z) {
    return false;
  }

  @Override
  public boolean isChunkGenerated(int x, int z) {
    return false;
  }

  @Override
  public boolean isChunkInUse(int x, int z) {
    return false;
  }

  @Override
  public void loadChunk(@NotNull Chunk chunk) {

  }

  @Override
  public void loadChunk(int x, int z) {

  }

  @Override
  public boolean loadChunk(int x, int z, boolean generate) {
    return false;
  }

  @Override
  public boolean unloadChunk(@NotNull Chunk chunk) {
    return false;
  }

  @Override
  public boolean unloadChunk(int x, int z) {
    return false;
  }

  @Override
  public boolean unloadChunk(int x, int z, boolean save) {
    return false;
  }

  @Override
  public boolean unloadChunkRequest(int x, int z) {
    return false;
  }

  @Override
  public boolean regenerateChunk(int x, int z) {
    return false;
  }

  @Override
  public boolean refreshChunk(int x, int z) {
    return false;
  }

  @Override
  public boolean isChunkForceLoaded(int x, int z) {
    return false;
  }

  @Override
  public void setChunkForceLoaded(int x, int z, boolean forced) {

  }

  @Override
  public @NotNull Collection<Chunk> getForceLoadedChunks() {
    return null;
  }

  @Override
  public boolean addPluginChunkTicket(int x, int z, @NotNull Plugin plugin) {
    return false;
  }

  @Override
  public boolean removePluginChunkTicket(int x, int z, @NotNull Plugin plugin) {
    return false;
  }

  @Override
  public void removePluginChunkTickets(@NotNull Plugin plugin) {

  }

  @Override
  public @NotNull Collection<Plugin> getPluginChunkTickets(int x, int z) {
    return null;
  }

  @Override
  public @NotNull Map<Plugin, Collection<Chunk>> getPluginChunkTickets() {
    return null;
  }

  @Override
  public @NotNull Item dropItem(@NotNull Location location, @NotNull ItemStack item) {
    return null;
  }

  @Override
  public @NotNull Item dropItem(@NotNull Location location,
                                @NotNull ItemStack item,
                                @Nullable Consumer<Item> function) {
    return null;
  }

  @Override
  public @NotNull Item dropItemNaturally(@NotNull Location location, @NotNull ItemStack item) {
    return null;
  }

  @Override
  public @NotNull Item dropItemNaturally(@NotNull Location location,
                                         @NotNull ItemStack item,
                                         @Nullable Consumer<Item> function) {
    return null;
  }

  @Override
  public @NotNull Arrow spawnArrow(@NotNull Location location,
                                   @NotNull Vector direction,
                                   float speed,
                                   float spread) {
    return null;
  }

  @Override
  public <T extends AbstractArrow> @NotNull T spawnArrow(@NotNull Location location,
                                                         @NotNull Vector direction,
                                                         float speed,
                                                         float spread,
                                                         @NotNull Class<T> clazz) {
    return null;
  }

  @Override
  public boolean generateTree(@NotNull Location location, @NotNull TreeType type) {
    return false;
  }

  @Override
  public boolean generateTree(@NotNull Location loc,
                              @NotNull TreeType type,
                              @NotNull BlockChangeDelegate delegate) {
    return false;
  }

  @Override
  public @NotNull Entity spawnEntity(@NotNull Location loc, @NotNull EntityType type) {
    return null;
  }

  @Override
  public @NotNull LightningStrike strikeLightning(@NotNull Location loc) {
    return null;
  }

  @Override
  public @NotNull LightningStrike strikeLightningEffect(@NotNull Location loc) {
    return null;
  }

  @Override
  public @NotNull List<Entity> getEntities() {
    return null;
  }

  @Override
  public @NotNull List<LivingEntity> getLivingEntities() {
    return null;
  }

  @Override
  public @NotNull <T extends Entity> Collection<T> getEntitiesByClass(
      @NotNull Class<T>... classes) {
    return null;
  }

  @Override
  public @NotNull <T extends Entity> Collection<T> getEntitiesByClass(@NotNull Class<T> cls) {
    return null;
  }

  @Override
  public @NotNull Collection<Entity> getEntitiesByClasses(@NotNull Class<?>... classes) {
    return null;
  }

  @Override
  public @NotNull List<Player> getPlayers() {
    return null;
  }

  @Override
  public @Nullable Entity getEntity(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public @NotNull Collection<Entity> getNearbyEntities(@NotNull Location location,
                                                       double x,
                                                       double y,
                                                       double z) {
    return null;
  }

  @Override
  public @NotNull Collection<Entity> getNearbyEntities(@NotNull Location location,
                                                       double x,
                                                       double y,
                                                       double z,
                                                       @Nullable Predicate<Entity> filter) {
    return null;
  }

  @Override
  public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox boundingBox) {
    return null;
  }

  @Override
  public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox boundingBox,
                                                       @Nullable Predicate<Entity> filter) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTraceEntities(@NotNull Location start,
                                                   @NotNull Vector direction,
                                                   double maxDistance) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTraceEntities(@NotNull Location start,
                                                   @NotNull Vector direction,
                                                   double maxDistance,
                                                   double raySize) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTraceEntities(@NotNull Location start,
                                                   @NotNull Vector direction,
                                                   double maxDistance,
                                                   @Nullable Predicate<Entity> filter) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTraceEntities(@NotNull Location start,
                                                   @NotNull Vector direction,
                                                   double maxDistance,
                                                   double raySize,
                                                   @Nullable Predicate<Entity> filter) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTraceBlocks(@NotNull Location start,
                                                 @NotNull Vector direction,
                                                 double maxDistance) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTraceBlocks(@NotNull Location start,
                                                 @NotNull Vector direction,
                                                 double maxDistance,
                                                 @NotNull FluidCollisionMode fluidCollisionMode) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTraceBlocks(@NotNull Location start,
                                                 @NotNull Vector direction,
                                                 double maxDistance,
                                                 @NotNull FluidCollisionMode fluidCollisionMode,
                                                 boolean ignorePassableBlocks) {
    return null;
  }

  @Override
  public @Nullable RayTraceResult rayTrace(@NotNull Location start,
                                           @NotNull Vector direction,
                                           double maxDistance,
                                           @NotNull FluidCollisionMode fluidCollisionMode,
                                           boolean ignorePassableBlocks,
                                           double raySize,
                                           @Nullable Predicate<Entity> filter) {
    return null;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public @NotNull UUID getUID() {
    return null;
  }

  @Override
  public @NotNull Location getSpawnLocation() {
    return null;
  }

  @Override
  public boolean setSpawnLocation(@NotNull Location location) {
    return false;
  }

  @Override
  public boolean setSpawnLocation(int x, int y, int z, float angle) {
    return false;
  }

  @Override
  public boolean setSpawnLocation(int x, int y, int z) {
    return false;
  }

  @Override
  public long getTime() {
    return 0;
  }

  @Override
  public void setTime(long time) {

  }

  @Override
  public long getFullTime() {
    return time;
  }

  @Override
  public void setFullTime(long time) {
    this.time = time;
  }

  @Override
  public boolean isDayTime() {
    return false;
  }

  @Override
  public long getGameTime() {
    return 0;
  }

  @Override
  public boolean hasStorm() {
    return false;
  }

  @Override
  public void setStorm(boolean hasStorm) {

  }

  @Override
  public int getWeatherDuration() {
    return 0;
  }

  @Override
  public void setWeatherDuration(int duration) {

  }

  @Override
  public boolean isThundering() {
    return false;
  }

  @Override
  public void setThundering(boolean thundering) {

  }

  @Override
  public int getThunderDuration() {
    return 0;
  }

  @Override
  public void setThunderDuration(int duration) {

  }

  @Override
  public boolean isClearWeather() {
    return false;
  }

  @Override
  public void setClearWeatherDuration(int duration) {

  }

  @Override
  public int getClearWeatherDuration() {
    return 0;
  }

  @Override
  public boolean createExplosion(double x, double y, double z, float power) {
    return false;
  }

  @Override
  public boolean createExplosion(double x, double y, double z, float power, boolean setFire) {
    return false;
  }

  @Override
  public boolean createExplosion(double x,
                                 double y,
                                 double z,
                                 float power,
                                 boolean setFire,
                                 boolean breakBlocks) {
    return false;
  }

  @Override
  public boolean createExplosion(double x,
                                 double y,
                                 double z,
                                 float power,
                                 boolean setFire,
                                 boolean breakBlocks,
                                 @Nullable Entity source) {
    return false;
  }

  @Override
  public boolean createExplosion(@NotNull Location loc, float power) {
    return false;
  }

  @Override
  public boolean createExplosion(@NotNull Location loc, float power, boolean setFire) {
    return false;
  }

  @Override
  public boolean createExplosion(@Nullable Entity source,
                                 @NotNull Location loc,
                                 float power,
                                 boolean setFire,
                                 boolean breakBlocks) {
    return false;
  }

  @Override
  public boolean createExplosion(@NotNull Location loc,
                                 float power,
                                 boolean setFire,
                                 boolean breakBlocks) {
    return false;
  }

  @Override
  public boolean createExplosion(@NotNull Location loc,
                                 float power,
                                 boolean setFire,
                                 boolean breakBlocks,
                                 @Nullable Entity source) {
    return false;
  }

  @Override
  public @NotNull Environment getEnvironment() {
    return null;
  }

  @Override
  public long getSeed() {
    return 0;
  }

  @Override
  public boolean getPVP() {
    return false;
  }

  @Override
  public void setPVP(boolean pvp) {

  }

  @Override
  public @Nullable ChunkGenerator getGenerator() {
    return null;
  }

  @Override
  public void save() {

  }

  @Override
  public @NotNull List<BlockPopulator> getPopulators() {
    return null;
  }

  @Override
  public <T extends Entity> @NotNull T spawn(@NotNull Location location,
                                             @NotNull Class<T> clazz)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public <T extends Entity> @NotNull T spawn(@NotNull Location location,
                                             @NotNull Class<T> clazz,
                                             @Nullable Consumer<T> function,
                                             CreatureSpawnEvent.@NotNull SpawnReason reason)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location,
                                                 @NotNull MaterialData data)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location,
                                                 @NotNull BlockData data)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location,
                                                 @NotNull Material material,
                                                 byte data) throws IllegalArgumentException {
    return null;
  }

  @Override
  public void playEffect(@NotNull Location location, @NotNull Effect effect, int data) {

  }

  @Override
  public void playEffect(@NotNull Location location, @NotNull Effect effect, int data, int radius) {

  }

  @Override
  public <T> void playEffect(@NotNull Location location, @NotNull Effect effect, @Nullable T data) {

  }

  @Override
  public <T> void playEffect(@NotNull Location location,
                             @NotNull Effect effect,
                             @Nullable T data,
                             int radius) {

  }

  @Override
  public @NotNull ChunkSnapshot getEmptyChunkSnapshot(int x,
                                                      int z,
                                                      boolean includeBiome,
                                                      boolean includeBiomeTemp) {
    return null;
  }

  @Override
  public void setSpawnFlags(boolean allowMonsters, boolean allowAnimals) {

  }

  @Override
  public boolean getAllowAnimals() {
    return false;
  }

  @Override
  public boolean getAllowMonsters() {
    return false;
  }

  @Override
  public @NotNull Biome getBiome(int x, int z) {
    return null;
  }

  @Override
  public @NotNull Biome getBiome(int x, int y, int z) {
    return null;
  }

  @Override
  public void setBiome(int x, int z, @NotNull Biome bio) {

  }

  @Override
  public void setBiome(int x, int y, int z, @NotNull Biome bio) {

  }

  @Override
  public double getTemperature(int x, int z) {
    return 0;
  }

  @Override
  public double getTemperature(int x, int y, int z) {
    return 0;
  }

  @Override
  public double getHumidity(int x, int z) {
    return 0;
  }

  @Override
  public double getHumidity(int x, int y, int z) {
    return 0;
  }

  @Override
  public int getMinHeight() {
    return 0;
  }

  @Override
  public int getMaxHeight() {
    return 0;
  }

  @Override
  public int getSeaLevel() {
    return 0;
  }

  @Override
  public boolean getKeepSpawnInMemory() {
    return false;
  }

  @Override
  public void setKeepSpawnInMemory(boolean keepLoaded) {

  }

  @Override
  public boolean isAutoSave() {
    return false;
  }

  @Override
  public void setAutoSave(boolean value) {

  }

  @Override
  public void setDifficulty(@NotNull Difficulty difficulty) {
    this.difficulty = difficulty;
  }

  @Override
  public @NotNull Difficulty getDifficulty() {
    return difficulty;
  }

  @Override
  public @NotNull File getWorldFolder() {
    return null;
  }

  @Override
  public @Nullable WorldType getWorldType() {
    return null;
  }

  @Override
  public boolean canGenerateStructures() {
    return false;
  }

  @Override
  public boolean isHardcore() {
    return false;
  }

  @Override
  public void setHardcore(boolean hardcore) {

  }

  @Override
  public long getTicksPerAnimalSpawns() {
    return 0;
  }

  @Override
  public void setTicksPerAnimalSpawns(int ticksPerAnimalSpawns) {

  }

  @Override
  public long getTicksPerMonsterSpawns() {
    return 0;
  }

  @Override
  public void setTicksPerMonsterSpawns(int ticksPerMonsterSpawns) {

  }

  @Override
  public long getTicksPerWaterSpawns() {
    return 0;
  }

  @Override
  public void setTicksPerWaterSpawns(int ticksPerWaterSpawns) {

  }

  @Override
  public long getTicksPerWaterAmbientSpawns() {
    return 0;
  }

  @Override
  public void setTicksPerWaterAmbientSpawns(int ticksPerAmbientSpawns) {

  }

  @Override
  public long getTicksPerAmbientSpawns() {
    return 0;
  }

  @Override
  public void setTicksPerAmbientSpawns(int ticksPerAmbientSpawns) {

  }

  @Override
  public int getMonsterSpawnLimit() {
    return 0;
  }

  @Override
  public void setMonsterSpawnLimit(int limit) {

  }

  @Override
  public int getAnimalSpawnLimit() {
    return 0;
  }

  @Override
  public void setAnimalSpawnLimit(int limit) {

  }

  @Override
  public int getWaterAnimalSpawnLimit() {
    return 0;
  }

  @Override
  public void setWaterAnimalSpawnLimit(int limit) {

  }

  @Override
  public int getWaterAmbientSpawnLimit() {
    return 0;
  }

  @Override
  public void setWaterAmbientSpawnLimit(int limit) {

  }

  @Override
  public int getAmbientSpawnLimit() {
    return 0;
  }

  @Override
  public void setAmbientSpawnLimit(int limit) {

  }

  @Override
  public void playSound(@NotNull Location location,
                        @NotNull Sound sound,
                        float volume,
                        float pitch) {

  }

  @Override
  public void playSound(@NotNull Location location,
                        @NotNull String sound,
                        float volume,
                        float pitch) {

  }

  @Override
  public void playSound(@NotNull Location location,
                        @NotNull Sound sound,
                        @NotNull SoundCategory category,
                        float volume,
                        float pitch) {

  }

  @Override
  public void playSound(@NotNull Location location,
                        @NotNull String sound,
                        @NotNull SoundCategory category,
                        float volume,
                        float pitch) {

  }

  @Override
  public @NotNull String[] getGameRules() {
    return new String[0];
  }

  @Override
  public boolean setGameRuleValue(@NotNull String rule, @NotNull String value) {
    return false;
  }

  @Override
  public boolean isGameRule(@NotNull String rule) {
    return false;
  }

  @Override
  public @Nullable String getGameRuleValue(@Nullable String rule) {
    return null;
  }

  @Override
  public <T> @Nullable T getGameRuleValue(@NotNull GameRule<T> rule) {
    return null;
  }

  @Override
  public <T> @Nullable T getGameRuleDefault(@NotNull GameRule<T> rule) {
    return null;
  }

  @Override
  public <T> boolean setGameRule(@NotNull GameRule<T> rule, @NotNull T newValue) {
    return false;
  }

  @Override
  public @NotNull WorldBorder getWorldBorder() {
    return border;
  }

  @Override
  public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count) {

  }

  @Override
  public void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle,
                                @NotNull Location location,
                                int count,
                                @Nullable T data) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle, double x,
                                double y,
                                double z,
                                int count,
                                @Nullable T data) {

  }

  @Override
  public void spawnParticle(@NotNull Particle particle,
                            @NotNull Location location,
                            int count,
                            double offsetX,
                            double offsetY,
                            double offsetZ) {

  }

  @Override
  public void spawnParticle(@NotNull Particle particle,
                            double x,
                            double y,
                            double z,
                            int count,
                            double offsetX,
                            double offsetY,
                            double offsetZ) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle,
                                @NotNull Location location,
                                int count,
                                double offsetX,
                                double offsetY,
                                double offsetZ,
                                @Nullable T data) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle,
                                double x,
                                double y,
                                double z,
                                int count,
                                double offsetX,
                                double offsetY,
                                double offsetZ,
                                @Nullable T data) {

  }

  @Override
  public void spawnParticle(@NotNull Particle particle,
                            @NotNull Location location,
                            int count,
                            double offsetX,
                            double offsetY,
                            double offsetZ,
                            double extra) {

  }

  @Override
  public void spawnParticle(@NotNull Particle particle,
                            double x,
                            double y,
                            double z,
                            int count,
                            double offsetX,
                            double offsetY,
                            double offsetZ,
                            double extra) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle,
                                @NotNull Location location,
                                int count,
                                double offsetX,
                                double offsetY,
                                double offsetZ,
                                double extra,
                                @Nullable T data) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle,
                                @Nullable List<Player> receivers,
                                @Nullable Player source,
                                double x,
                                double y,
                                double z,
                                int count,
                                double offsetX,
                                double offsetY,
                                double offsetZ,
                                double extra,
                                @Nullable T data,
                                boolean force) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle,
                                @NotNull Location location,
                                int count,
                                double offsetX,
                                double offsetY,
                                double offsetZ,
                                double extra,
                                @Nullable T data,
                                boolean force) {

  }

  @Override
  public <T> void spawnParticle(@NotNull Particle particle,
                                double x,
                                double y,
                                double z,
                                int count,
                                double offsetX,
                                double offsetY,
                                double offsetZ,
                                double extra,
                                @Nullable T data,
                                boolean force) {

  }

  @Override
  public @Nullable Location locateNearestStructure(@NotNull Location origin,
                                                   @NotNull StructureType structureType,
                                                   int radius,
                                                   boolean findUnexplored) {
    return null;
  }

  @Override
  public @Nullable Location locateNearestBiome(@NotNull Location origin,
                                               @NotNull Biome biome,
                                               int radius) {
    return null;
  }

  @Override
  public @Nullable Location locateNearestBiome(@NotNull Location origin,
                                               @NotNull Biome biome,
                                               int radius,
                                               int step) {
    return null;
  }

  @Override
  public boolean isUltrawarm() {
    return false;
  }

  @Override
  public boolean isNatural() {
    return false;
  }

  @Override
  public double getCoordinateScale() {
    return 0;
  }

  @Override
  public boolean hasSkylight() {
    return false;
  }

  @Override
  public boolean hasBedrockCeiling() {
    return false;
  }

  @Override
  public boolean isPiglinSafe() {
    return false;
  }

  @Override
  public boolean doesBedWork() {
    return false;
  }

  @Override
  public boolean doesRespawnAnchorWork() {
    return false;
  }

  @Override
  public boolean hasRaids() {
    return false;
  }

  @Override
  public boolean isFixedTime() {
    return false;
  }

  @Override
  public @NotNull Collection<Material> getInfiniburn() {
    return null;
  }

  @Override
  public int getViewDistance() {
    return 0;
  }

  @Override
  public void setViewDistance(int viewDistance) {

  }

  @Override
  public int getNoTickViewDistance() {
    return 0;
  }

  @Override
  public void setNoTickViewDistance(int viewDistance) {

  }

  @Override
  public @NotNull Spigot spigot() {
    return null;
  }

  @Override
  public @Nullable Raid locateNearestRaid(@NotNull Location location, int radius) {
    return null;
  }

  @Override
  public @NotNull List<Raid> getRaids() {
    return null;
  }

  @Override
  public @Nullable DragonBattle getEnderDragonBattle() {
    return null;
  }

  @Override
  public void setMetadata(@NotNull String metadataKey, @NotNull MetadataValue newMetadataValue) {

  }

  @Override
  public @NotNull List<MetadataValue> getMetadata(@NotNull String metadataKey) {
    return null;
  }

  @Override
  public boolean hasMetadata(@NotNull String metadataKey) {
    return false;
  }

  @Override
  public void removeMetadata(@NotNull String metadataKey, @NotNull Plugin owningPlugin) {

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
