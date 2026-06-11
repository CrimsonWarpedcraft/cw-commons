package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crimsonwarpedcraft.cwcommons.store.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerDataManagerTest {

  private Repository<UUID, Object> mockRepo;
  private Plugin mockPlugin;
  private PluginManager mockPluginManager;
  private Player mockPlayer;
  private UUID playerId;
  private PlayerDataManager<Object> manager;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockRepo = mock(Repository.class);
    mockPlugin = mock(Plugin.class);
    mockPluginManager = mock(PluginManager.class);
    mockPlayer = mock(Player.class);
    playerId = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(playerId);
    when(mockRepo.get(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(mockRepo.put(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(mockRepo.flush()).thenReturn(CompletableFuture.completedFuture(null));
    Server mockServer = mock(Server.class);
    when(mockPlugin.getServer()).thenReturn(mockServer);
    when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
    manager = new PlayerDataManager<>(mockRepo, mockPlugin);
  }

  @Test
  void nullRepositoryThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> new PlayerDataManager<>(null, mockPlugin));
  }

  @Test
  void nullPluginThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> new PlayerDataManager<>(mockRepo, null));
  }

  @Test
  void registerEventsRegistersWithPluginManager() {
    manager.registerEvents();
    verify(mockPluginManager).registerEvents(any(PlayerDataManager.class), eq(mockPlugin));
  }

  @Test
  void getCallsRepositoryWithPlayerUuid() throws Exception {
    manager.get(mockPlayer).get();
    verify(mockRepo).get(playerId);
  }

  @Test
  void getReturnsValueFromRepository() throws Exception {
    Object data = new Object();
    when(mockRepo.get(playerId))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(data)));

    Optional<Object> result = manager.get(mockPlayer).get();

    assertTrue(result.isPresent());
    assertSame(data, result.get());
  }

  @Test
  void getNullPlayerThrowsNpe() {
    assertThrows(NullPointerException.class, () -> manager.get(null));
  }

  @Test
  void saveCallsRepositoryPutWithPlayerUuid() throws Exception {
    Object data = new Object();
    manager.save(mockPlayer, data).get();
    verify(mockRepo).put(playerId, data);
  }

  @Test
  void saveReturnsFutureFromRepository() throws Exception {
    CompletableFuture<Void> repoFuture = new CompletableFuture<>();
    when(mockRepo.put(eq(playerId), any())).thenReturn(repoFuture);

    CompletableFuture<Void> result = manager.save(mockPlayer, new Object());

    assertSame(repoFuture, result);
  }

  @Test
  void saveNullPlayerThrowsNpe() {
    assertThrows(NullPointerException.class, () -> manager.save(null, new Object()));
  }

  @Test
  void saveNullDataThrowsNpe() {
    assertThrows(NullPointerException.class, () -> manager.save(mockPlayer, null));
  }

  @Test
  void onPlayerQuitFlushesRepository() {
    manager.onPlayerQuit(mock(PlayerQuitEvent.class));
    verify(mockRepo).flush();
  }
}
