package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AutoFlushTaskTest {

  private DataStore mockStore;
  private Plugin mockPlugin;
  private Server mockServer;
  private BukkitScheduler mockScheduler;
  private BukkitTask mockTask;

  @BeforeEach
  void setUp() {
    mockStore = mock(DataStore.class);
    mockPlugin = mock(Plugin.class);
    mockServer = mock(Server.class);
    mockScheduler = mock(BukkitScheduler.class);
    mockTask = mock(BukkitTask.class);
    when(mockPlugin.getServer()).thenReturn(mockServer);
    when(mockServer.getScheduler()).thenReturn(mockScheduler);
    when(mockScheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong()))
        .thenReturn(mockTask);
    when(mockScheduler.runTask(any(), any(Runnable.class))).thenReturn(mockTask);
    when(mockStore.flush()).thenReturn(CompletableFuture.completedFuture(null));
  }

  @Test
  void nullStoreThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new AutoFlushTask(null, mockPlugin));
  }

  @Test
  void nullPluginThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new AutoFlushTask(mockStore, null));
  }

  @Test
  void nullOnFlushThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> new AutoFlushTask(mockStore, mockPlugin, AutoFlushTask.DEFAULT_INTERVAL_TICKS, null));
  }

  @Test
  void startSchedulesTimerWithDefaultInterval() {
    new AutoFlushTask(mockStore, mockPlugin).start();
    verify(mockScheduler).runTaskTimer(
        eq(mockPlugin), any(Runnable.class),
        eq(AutoFlushTask.DEFAULT_INTERVAL_TICKS), eq(AutoFlushTask.DEFAULT_INTERVAL_TICKS));
  }

  @Test
  void startSchedulesTimerWithCustomInterval() {
    new AutoFlushTask(mockStore, mockPlugin, 100L).start();
    verify(mockScheduler).runTaskTimer(
        eq(mockPlugin), any(Runnable.class), eq(100L), eq(100L));
  }

  @Test
  void startReturnsBukkitTask() {
    BukkitTask result = new AutoFlushTask(mockStore, mockPlugin).start();
    assertSame(mockTask, result);
  }

  @Test
  void timerTickFlushesStore() {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    new AutoFlushTask(mockStore, mockPlugin).start();
    verify(mockScheduler).runTaskTimer(any(), captor.capture(), anyLong(), anyLong());
    captor.getValue().run();
    verify(mockStore).flush();
  }

  @Test
  void timerTickWithCallbackSchedulesCallbackOnMainThread() {
    Runnable onFlush = mock(Runnable.class);
    ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
    new AutoFlushTask(
        mockStore, mockPlugin, AutoFlushTask.DEFAULT_INTERVAL_TICKS, onFlush).start();
    verify(mockScheduler).runTaskTimer(any(), timerCaptor.capture(), anyLong(), anyLong());
    timerCaptor.getValue().run();
    verify(mockScheduler).runTask(eq(mockPlugin), eq(onFlush));
  }

  @Test
  void timerTickWithCallbackRunsCallback() {
    Runnable onFlush = mock(Runnable.class);
    ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
    new AutoFlushTask(
        mockStore, mockPlugin, AutoFlushTask.DEFAULT_INTERVAL_TICKS, onFlush).start();
    verify(mockScheduler).runTaskTimer(any(), timerCaptor.capture(), anyLong(), anyLong());
    timerCaptor.getValue().run();
    ArgumentCaptor<Runnable> mainCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(mockScheduler).runTask(any(), mainCaptor.capture());
    mainCaptor.getValue().run();
    verify(onFlush).run();
  }
}
