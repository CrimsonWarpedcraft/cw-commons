package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class AutoFlushTaskTest {

  private DataStore mockStore;
  private Plugin mockPlugin;
  private Server mockServer;
  private BukkitScheduler mockScheduler;
  private BukkitTask mockTask;
  private Logger mockLogger;
  private Runnable onFlush;
  private CompletableFuture<Void> flushFuture;

  @BeforeEach
  void setUp() {
    mockStore = mock(DataStore.class);
    mockPlugin = mock(Plugin.class);
    mockServer = mock(Server.class);
    mockScheduler = mock(BukkitScheduler.class);
    mockTask = mock(BukkitTask.class);
    mockLogger = mock(Logger.class);
    onFlush = mock(Runnable.class);
    flushFuture = new CompletableFuture<>();
    when(mockPlugin.getServer()).thenReturn(mockServer);
    when(mockPlugin.getLogger()).thenReturn(mockLogger);
    when(mockPlugin.isEnabled()).thenReturn(true);
    when(mockServer.getScheduler()).thenReturn(mockScheduler);
    when(mockScheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong()))
        .thenReturn(mockTask);
    when(mockScheduler.runTask(any(), any(Runnable.class))).thenReturn(mockTask);
    when(mockStore.flush()).thenReturn(flushFuture);
  }

  @Test
  void nullStoreThrowsNpe() {
    assertThrows(NullPointerException.class, () -> AutoFlushTask.builder(null, mockPlugin));
  }

  @Test
  void nullPluginThrowsNpe() {
    assertThrows(NullPointerException.class, () -> AutoFlushTask.builder(mockStore, null));
  }

  @Test
  void nullOnFlushThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> AutoFlushTask.builder(mockStore, mockPlugin).onFlush(null));
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
  void nonPositiveBuilderIntervalsAreRejected(long interval) {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> AutoFlushTask.builder(mockStore, mockPlugin).interval(interval));
    assertTrue(failure.getMessage().contains("positive"));
    verifyNoInteractions(mockScheduler);
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
  void nonPositiveTaskIntervalsAreRejected(long interval) {
    assertThrows(IllegalArgumentException.class,
        () -> new AutoFlushTask(mockStore, mockPlugin, interval, onFlush));
  }

  @Test
  void startSchedulesTimerWithDefaultInterval() {
    AutoFlushTask.builder(mockStore, mockPlugin).build().start();
    verify(mockScheduler).runTaskTimer(
        eq(mockPlugin), any(Runnable.class),
        eq(AutoFlushTask.DEFAULT_INTERVAL_TICKS), eq(AutoFlushTask.DEFAULT_INTERVAL_TICKS));
  }

  @Test
  void startSchedulesTimerWithCustomInterval() {
    AutoFlushTask.builder(mockStore, mockPlugin).interval(100L).build().start();
    verify(mockScheduler).runTaskTimer(
        eq(mockPlugin), any(Runnable.class), eq(100L), eq(100L));
  }

  @Test
  void oneTickIntervalIsAccepted() {
    AutoFlushTask.builder(mockStore, mockPlugin).interval(1L).build().start();
    verify(mockScheduler).runTaskTimer(
        eq(mockPlugin), any(Runnable.class), eq(1L), eq(1L));
  }

  @Test
  void startReturnsBukkitTask() {
    BukkitTask result = AutoFlushTask.builder(mockStore, mockPlugin).build().start();
    assertSame(mockTask, result);
  }

  @Test
  void callbackWaitsForSuccessfulCompletionAndMainThreadExecution() {
    Runnable timer = startTimer();
    verifyNoInteractions(mockStore, onFlush, mockLogger);

    timer.run();
    verify(mockStore).flush();
    assertFalse(flushFuture.isDone());
    verify(mockScheduler, never()).runTask(eq(mockPlugin), any(Runnable.class));
    verifyNoInteractions(onFlush, mockLogger);

    clearInvocations(mockPlugin, mockServer);
    flushFuture.complete(null);
    verifyNoInteractions(onFlush, mockLogger, mockServer);
    verify(mockPlugin).isEnabled();
    verifyNoMoreInteractions(mockPlugin);

    Runnable callback = scheduledCallback();
    callback.run();
    verify(onFlush).run();
    verifyNoMoreInteractions(onFlush);
    verifyNoInteractions(mockLogger);
  }

  @Test
  void delayedFailureIsLoggedOnceWithoutSchedulingCallback() {
    startTimer().run();
    verifyNoInteractions(mockLogger);

    IOException failure = new IOException("disk unavailable");
    assertTrue(flushFuture.completeExceptionally(failure));
    assertFalse(flushFuture.completeExceptionally(new IOException("second completion")));

    verifyFlushFailure(failure);
  }

  @Test
  void alreadyFailedFutureIsLoggedWithItsCauseOnce() {
    CompletionException failure = new CompletionException(new IOException("flush failed"));
    when(mockStore.flush()).thenReturn(CompletableFuture.failedFuture(failure));

    startTimer().run();

    verifyFlushFailure(failure);
  }

  @Test
  void disableBeforeCompletionSkipsCallbackScheduling() {
    startTimer().run();
    when(mockPlugin.isEnabled()).thenReturn(false);

    flushFuture.complete(null);

    verify(mockScheduler, never()).runTask(eq(mockPlugin), any(Runnable.class));
    verifyNoInteractions(onFlush, mockLogger);
  }

  @Test
  void disableBeforeExecutionSkipsScheduledCallback() {
    startTimer().run();
    flushFuture.complete(null);
    Runnable callback = scheduledCallback();
    when(mockPlugin.isEnabled()).thenReturn(false);

    callback.run();

    verifyNoInteractions(onFlush, mockLogger);
  }

  @Test
  void failureAfterDisableIsStillLogged() {
    startTimer().run();
    when(mockPlugin.isEnabled()).thenReturn(false);
    clearInvocations(mockPlugin, mockServer);

    IOException failure = new IOException("flush failed during disable");
    flushFuture.completeExceptionally(failure);

    verifyFlushFailure(failure);
    verifyNoInteractions(mockPlugin, mockServer);
  }

  @Test
  void schedulerRejectionDuringDisableIsReportedOnce() {
    startTimer().run();
    IllegalPluginAccessException failure = new IllegalPluginAccessException("plugin disabled");
    when(mockScheduler.runTask(eq(mockPlugin), any(Runnable.class))).thenAnswer(invocation -> {
      when(mockPlugin.isEnabled()).thenReturn(false);
      throw failure;
    });

    flushFuture.complete(null);

    verify(mockScheduler).runTask(eq(mockPlugin), any(Runnable.class));
    verify(mockLogger).log(
        eq(Level.WARNING), contains("auto-flush success callback"), same(failure));
    verifyNoMoreInteractions(mockLogger);
    verifyNoInteractions(onFlush);
  }

  @Test
  void synchronousFlushRejectionIsReportedOnce() {
    RejectedExecutionException failure = new RejectedExecutionException("store closed");
    when(mockStore.flush()).thenThrow(failure);

    assertDoesNotThrow(startTimer()::run);

    verifyFlushFailure(failure);
  }

  @Test
  void synchronousTimerSchedulingFailureRemainsCallerOwned() {
    IllegalPluginAccessException failure = new IllegalPluginAccessException("plugin disabled");
    when(mockScheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong()))
        .thenThrow(failure);

    assertSame(failure, assertThrows(IllegalPluginAccessException.class,
        () -> AutoFlushTask.builder(mockStore, mockPlugin).build().start()));
    verifyNoInteractions(mockStore, onFlush, mockLogger);
  }

  private Runnable startTimer() {
    AutoFlushTask.builder(mockStore, mockPlugin).onFlush(onFlush).build().start();
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(mockScheduler).runTaskTimer(
        eq(mockPlugin), captor.capture(), anyLong(), anyLong());
    return captor.getValue();
  }

  private Runnable scheduledCallback() {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(mockScheduler).runTask(eq(mockPlugin), captor.capture());
    return captor.getValue();
  }

  private void verifyFlushFailure(Throwable failure) {
    verify(mockLogger).log(eq(Level.SEVERE), contains("periodic auto-flush"), same(failure));
    verifyNoMoreInteractions(mockLogger);
    verify(mockScheduler, never()).runTask(eq(mockPlugin), any(Runnable.class));
    verifyNoInteractions(onFlush);
  }
}
