package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.crimsonwarpedcraft.cwcommons.store.Repository;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  private Logger mockLogger;
  private PlayerQuitEvent quitEvent;
  private CompletableFuture<Void> flushFuture;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockRepo = mock(Repository.class);
    mockPlugin = mock(Plugin.class);
    mockPluginManager = mock(PluginManager.class);
    mockPlayer = mock(Player.class);
    mockLogger = mock(Logger.class);
    quitEvent = mock(PlayerQuitEvent.class);
    flushFuture = new CompletableFuture<>();
    when(quitEvent.getPlayer()).thenReturn(mockPlayer);
    playerId = UUID.randomUUID();
    when(mockPlayer.getUniqueId()).thenReturn(playerId);
    when(mockRepo.get(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(mockRepo.put(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(mockRepo.flush()).thenReturn(flushFuture);
    Server mockServer = mock(Server.class);
    when(mockPlugin.getServer()).thenReturn(mockServer);
    when(mockPlugin.getLogger()).thenReturn(mockLogger);
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
  void constructionDoesNotRegisterListener() {
    verifyNoInteractions(mockPluginManager);
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
  void getReturnsCallerOwnedFailureWithoutLogging() {
    CompletableFuture<Optional<Object>> future = new CompletableFuture<>();
    when(mockRepo.get(playerId)).thenReturn(future);
    assertSame(future, manager.get(mockPlayer));

    IOException failure = new IOException("read failed");
    future.completeExceptionally(failure);

    assertSame(failure, assertThrows(CompletionException.class, future::join).getCause());
    verifyNoInteractions(mockLogger);
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
  void saveReturnsCallerOwnedFailureWithoutLogging() {
    IOException failure = new IOException("write failed");
    CompletableFuture<Void> future = CompletableFuture.failedFuture(failure);
    when(mockRepo.put(eq(playerId), any())).thenReturn(future);

    assertSame(future, manager.save(mockPlayer, new Object()));
    assertSame(failure, assertThrows(CompletionException.class, future::join).getCause());
    verifyNoInteractions(mockLogger);
  }

  @Test
  void updateCapturesUuidAndReturnsRepositoryFuture() {
    Function<Optional<Object>, Object> transform = current -> current.orElseGet(Object::new);
    CompletableFuture<Object> future = new CompletableFuture<>();
    when(mockRepo.update(playerId, transform)).thenReturn(future);
    assertSame(future, manager.update(mockPlayer, transform));
    verify(mockPlayer).getUniqueId();
    verify(mockRepo).update(playerId, transform);
    verifyNoMoreInteractions(mockPlayer, mockRepo);
    assertFalse(future.isDone());
  }

  @Test
  void onPlayerQuitFlushesWholeRepositoryWithoutWaiting() {
    manager.onPlayerQuit(quitEvent);

    verify(mockRepo).flush();
    verifyNoMoreInteractions(mockRepo);
    assertFalse(flushFuture.isDone());
    verifyNoInteractions(mockLogger);

    flushFuture.complete(null);

    verifyNoInteractions(mockLogger);
  }

  @Test
  void quitFailureAfterDisableIsLoggedOnceWithPlayerContext() {
    manager.onPlayerQuit(quitEvent);
    verifyNoInteractions(mockLogger);
    when(mockPlugin.isEnabled()).thenReturn(false);
    clearInvocations(mockPlugin, mockPlayer);

    IOException failure = new IOException("disk unavailable");
    assertTrue(flushFuture.completeExceptionally(failure));
    assertFalse(flushFuture.completeExceptionally(new IOException("second completion")));

    verifyQuitFailure(failure);
    verifyNoInteractions(mockPlugin, mockPlayer);
  }

  @Test
  void alreadyFailedQuitFlushIsLoggedWithItsCauseOnce() {
    CompletionException failure = new CompletionException(new IOException("flush failed"));
    when(mockRepo.flush()).thenReturn(CompletableFuture.failedFuture(failure));

    manager.onPlayerQuit(quitEvent);

    verifyQuitFailure(failure);
  }

  @Test
  void synchronousQuitFlushRejectionIsReportedOnce() {
    RejectedExecutionException failure = new RejectedExecutionException("store closed");
    when(mockRepo.flush()).thenThrow(failure);

    assertDoesNotThrow(() -> manager.onPlayerQuit(quitEvent));

    verifyQuitFailure(failure);
  }

  private void verifyQuitFailure(Throwable failure) {
    verify(mockLogger).log(eq(Level.SEVERE),
        eq("Failed to flush player data repository after quit for player " + playerId),
        same(failure));
    verifyNoMoreInteractions(mockLogger);
  }
}
