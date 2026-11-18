package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.InOrder;

@Timeout(10)
class StoreLifecycleTest {

  @Test
  void closeDrainsEveryAdmittedOperationBeforeFlushAndBackendRelease() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    when(backend.load("ns", "first")).thenReturn(Optional.empty());
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = rawStore(backend, executor, Duration.ZERO);
    Repository<String, String> repo = repository(store, "ns");
    final CompletableFuture<Void> first = repo.put("first", "one");
    final CompletableFuture<Optional<String>> read = repo.get("first");
    final CompletableFuture<Void> second = repo.put("second", "two");
    final CompletableFuture<Void> deletion = repo.delete("first");
    final CompletableFuture<Void> flush = store.flush();

    IOException timeout = assertThrows(IOException.class, store::close);
    assertInstanceOf(TimeoutException.class, timeout.getCause());
    verifyNoInteractions(backend);
    assertEquals(1, executor.pending());
    executor.runNext();
    CompletableFuture.allOf(first, read, second, deletion, flush).get(2, TimeUnit.SECONDS);
    store.close();

    InOrder order = inOrder(backend);
    order.verify(backend).save("ns", "first", "\"one\"");
    order.verify(backend).load("ns", "first");
    order.verify(backend).save("ns", "second", "\"two\"");
    order.verify(backend).delete("ns", "first");
    order.verify(backend, times(2)).flush("ns");
    order.verify(backend).close();
    order.verifyNoMoreInteractions();
  }

  @Test
  void queuedIncrementsFromMissingKeyAreAtomicAndDrainBeforeClose() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = DataStore.builder(backend).executor(executor).closeTimeout(Duration.ZERO)
        .build();
    Repository<String, Integer> counts = store.repository("counts", Integer.class,
        KeySerializers.forString());
    List<CompletableFuture<Integer>> results = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      results.add(counts.update("key", old -> old.orElse(0) + 1));
    }
    final CompletableFuture<Optional<Integer>> read = counts.get("key");
    assertThrows(IOException.class, store::close);
    assertRejected(counts.update("key", old -> -1));
    verifyNoInteractions(backend);
    executor.runNext();
    store.close();

    for (int i = 0; i < results.size(); i++) {
      assertEquals(i + 1, results.get(i).get(2, TimeUnit.SECONDS));
    }
    assertEquals(Optional.of(100), read.get(2, TimeUnit.SECONDS));
    assertRejected(counts.update("key", old -> -1));
    InOrder order = inOrder(backend);
    order.verify(backend).load("counts", "key");
    order.verify(backend).save("counts", "key", "100");
    order.verify(backend).flush("counts");
    order.verify(backend).close();
    order.verifyNoMoreInteractions();
  }

  @Test
  void closingRejectsEveryOperationAndBothNewAndExistingNamespaceLookups() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = rawStore(backend, executor, Duration.ZERO);
    Repository<String, String> repo = repository(store, "ns");
    assertThrows(IOException.class, store::close);

    assertRejected(repo.get("key"));
    assertRejected(repo.getAll());
    assertRejected(repo.put("key", "value"));
    assertRejected(repo.delete("key"));
    assertRejected(repo.flush());
    assertRejected(store.flush());
    assertThrows(RejectedExecutionException.class, () -> repository(store, "ns"));
    assertThrows(RejectedExecutionException.class, () -> repository(store, "new"));
    executor.runNext();
    store.close();
    assertRejected(repo.put("key", "value"));
    verify(backend, never()).save(anyString(), anyString(), anyString());
    verify(backend).flush("ns");
    verify(backend).close();
  }

  @Test
  void cancellationOfAcknowledgementDoesNotDropAnAdmittedWrite() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = rawStore(backend, executor, Duration.ZERO);
    CompletableFuture<Void> write = repository(store, "ns").put("key", "value");
    assertTrue(write.cancel(false));
    assertThrows(IOException.class, store::close);
    executor.runNext();
    store.close();
    verify(backend).save("ns", "key", "\"value\"");
    verify(backend).close();
  }

  @Test
  void bufferedWritesStillDrainAndPersistWhenBackendOwnershipIsRetained() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = DataStore.builder(backend).executor(executor).closeBackend(false)
        .closeTimeout(Duration.ZERO).build();
    final CompletableFuture<Void> write = repository(store, "ns").put("key", "value");
    assertThrows(IOException.class, store::close);
    verifyNoInteractions(backend);
    executor.runNext();
    write.get(2, TimeUnit.SECONDS);
    store.close();
    verify(backend).save("ns", "key", "\"value\"");
    verify(backend, never()).close();
  }

  @Test
  void multiWorkerExecutorStillHasExactlyOneLogicalLaneAcrossNamespaces() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    doAnswer(invocation -> {
      entered.countDown();
      await(release);
      return null;
    }).when(backend).save("first", "key", "\"one\"");
    ExecutorService workers = Executors.newFixedThreadPool(2);
    AtomicInteger dispatches = new AtomicInteger();
    Executor executor = task -> {
      dispatches.incrementAndGet();
      workers.execute(task);
    };
    DataStore store = rawStore(backend, executor, Duration.ofSeconds(2));
    try {
      final CompletableFuture<Void> first = repository(store, "first").put("key", "one");
      await(entered);
      CompletableFuture<Void> second = repository(store, "second").put("key", "two");
      CompletableFuture<Void> flush = store.flush();
      assertFalse(second.isDone());
      assertFalse(flush.isDone());
      assertEquals(1, dispatches.get());
      verify(backend, never()).save("second", "key", "\"two\"");
      release.countDown();
      CompletableFuture.allOf(first, second, flush).get(2, TimeUnit.SECONDS);
      store.close();
      assertFalse(workers.isShutdown());
      assertEquals(42, workers.submit(() -> 42).get(2, TimeUnit.SECONDS));
      InOrder order = inOrder(backend);
      order.verify(backend).save("first", "key", "\"one\"");
      order.verify(backend).save("second", "key", "\"two\"");
    } finally {
      release.countDown();
      shutdown(workers);
    }
  }

  @Test
  void concurrentCloseAndRepeatedClosePerformCleanupOnlyOnce() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = rawStore(backend, executor, Duration.ofSeconds(2));
    repository(store, "ns");
    ExecutorService callers = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      final Future<Void> first = callers.submit(() -> {
        ready.countDown();
        await(start);
        store.close();
        return null;
      });
      final Future<Void> second = callers.submit(() -> {
        ready.countDown();
        await(start);
        store.close();
        return null;
      });
      await(ready);
      start.countDown();
      executor.runNext();
      first.get(2, TimeUnit.SECONDS);
      second.get(2, TimeUnit.SECONDS);
      store.close();
      verify(backend).flush("ns");
      verify(backend).close();
      assertEquals(0, executor.pending());
    } finally {
      start.countDown();
      shutdown(callers);
    }
  }

  @Test
  void namespaceRegistrationAndCloseUseTheSameAdmissionGate() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    ThreadedRepositoryBuilder builder = new ThreadedRepositoryBuilder(
        backend, executor, new ObjectMapper(), true);
    DataStore store = new ConcurrentDataStore(builder);
    ExecutorService callers = Executors.newFixedThreadPool(2);
    CountDownLatch registering = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch closing = new CountDownLatch(1);
    try {
      final Future<Repository<String, String>> registration = callers.submit(
          () -> builder.whileOpen(() -> {
            registering.countDown();
            await(release);
            return repository(store, "racing");
          }));
      await(registering);
      final Future<Void> close = callers.submit(() -> {
        closing.countDown();
        store.close();
        return null;
      });
      await(closing);
      release.countDown();
      assertNotNull(registration.get(2, TimeUnit.SECONDS));
      executor.runNext();
      close.get(2, TimeUnit.SECONDS);
      verify(backend).flush("racing");
      verify(backend).close();
      assertThrows(RejectedExecutionException.class, () -> repository(store, "late"));
    } finally {
      release.countDown();
      shutdown(callers);
    }
  }

  @Test
  void timedOutCloseDoesNotCloseBackendUnderActiveIoOrDiscardQueuedWrites() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<Thread> worker = new AtomicReference<>();
    doAnswer(invocation -> {
      worker.set(Thread.currentThread());
      entered.countDown();
      await(release);
      return null;
    }).when(backend).save("ns", "first", "\"one\"");
    DataStore store = DataStore.builder(backend).cacheMode(CacheMode.NONE)
        .closeTimeout(Duration.ZERO).build();
    Repository<String, String> repo = repository(store, "ns");
    try {
      final CompletableFuture<Void> first = repo.put("first", "one");
      await(entered);
      final CompletableFuture<Void> second = repo.put("second", "two");
      IOException timeout = assertThrows(IOException.class, store::close);
      assertInstanceOf(TimeoutException.class, timeout.getCause());
      assertTrue(timeout.getMessage().contains("incomplete"));
      assertFalse(first.isDone());
      assertFalse(second.isDone());
      verify(backend, never()).flush("ns");
      verify(backend, never()).close();
      assertRejected(repo.put("late", "value"));
      release.countDown();
      CompletableFuture.allOf(first, second).get(2, TimeUnit.SECONDS);
    } finally {
      release.countDown();
      Thread thread = worker.get();
      if (thread != null) {
        thread.join(2000);
        assertFalse(thread.isAlive());
      }
    }
    store.close();
    InOrder order = inOrder(backend);
    order.verify(backend).save("ns", "first", "\"one\"");
    order.verify(backend).save("ns", "second", "\"two\"");
    order.verify(backend).flush("ns");
    order.verify(backend).close();
  }

  @Test
  void interruptedWaitReportsIncompletePersistencePreservesInterruptAndLeavesCleanupQueued()
      throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = rawStore(backend, executor, Duration.ofSeconds(2));
    CompletableFuture<Void> write = repository(store, "ns").put("key", "value");
    try {
      Thread.currentThread().interrupt();
      IOException interrupted = assertThrows(IOException.class, store::close);
      assertInstanceOf(InterruptedException.class, interrupted.getCause());
      assertTrue(interrupted.getMessage().contains("incomplete"));
      assertTrue(Thread.currentThread().isInterrupted());
      assertFalse(write.isDone());
      verifyNoInteractions(backend);
    } finally {
      Thread.interrupted();
    }
    assertRejected(store.flush());
    executor.runNext();
    write.get(2, TimeUnit.SECONDS);
    store.close();
    verify(backend).save("ns", "key", "\"value\"");
    verify(backend).close();
  }

  @Test
  void closeOnOwnWorkerFailsClearlyWithoutBeginningClosure() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    DataStore store = DataStore.builder(backend).cacheMode(CacheMode.NONE).build();
    doAnswer(invocation -> {
      IOException failure = assertThrows(IOException.class, store::close);
      assertTrue(failure.getMessage().contains("own execution context"));
      return null;
    }).when(backend).save(anyString(), anyString(), anyString());
    try {
      Repository<String, String> repo = repository(store, "ns");
      repo.put("first", "one").get(2, TimeUnit.SECONDS);
      repo.put("second", "two").get(2, TimeUnit.SECONDS);
    } finally {
      store.close();
    }
    verify(backend).close();
  }

  @Test
  void inlineContinuationCannotCloseButAttachingCallerCan() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ControlledExecutor executor = new ControlledExecutor();
    DataStore store = rawStore(backend, executor, Duration.ZERO);
    CompletableFuture<Void> write = repository(store, "ns").put("key", "value");
    CompletableFuture<Void> inline = write.thenRun(() -> {
      IOException failure = assertThrows(IOException.class, store::close);
      assertTrue(failure.getMessage().contains("own execution context"));
    });
    executor.runNext();
    inline.get(2, TimeUnit.SECONDS);
    AtomicReference<Thread> callback = new AtomicReference<>();
    write.thenRun(() -> {
      callback.set(Thread.currentThread());
      IOException pending = assertThrows(IOException.class, store::close);
      assertInstanceOf(TimeoutException.class, pending.getCause());
    }).get(2, TimeUnit.SECONDS);
    assertSame(Thread.currentThread(), callback.get());
    executor.runNext();
    store.close();
  }

  @Test
  void directExecutorRemainsSupportedAndDoesNotRecurseForQueuedWork() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    DataStore store = rawStore(backend, Runnable::run, Duration.ofSeconds(2));
    Repository<String, String> repo = repository(store, "ns");
    AtomicReference<CompletableFuture<Void>> nested = new AtomicReference<>();
    doAnswer(invocation -> {
      nested.set(repo.put("second", "two"));
      assertFalse(nested.get().isDone());
      return null;
    }).when(backend).save("ns", "first", "\"one\"");
    repo.put("first", "one").get(2, TimeUnit.SECONDS);
    nested.get().get(2, TimeUnit.SECONDS);
    store.close();
    InOrder order = inOrder(backend);
    order.verify(backend).save("ns", "first", "\"one\"");
    order.verify(backend).save("ns", "second", "\"two\"");
    order.verify(backend).flush("ns");
    order.verify(backend).close();
  }

  @Test
  void defaultOwnedWorkerReallyTerminatesEvenWhenBackendIsRetained() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    AtomicReference<Thread> worker = new AtomicReference<>();
    doAnswer(invocation -> {
      worker.set(Thread.currentThread());
      return null;
    }).when(backend).save(anyString(), anyString(), anyString());
    DataStore store = DataStore.builder(backend).name("lifecycle-test")
        .cacheMode(CacheMode.NONE).closeBackend(false).build();
    try {
      repository(store, "ns").put("key", "value").get(2, TimeUnit.SECONDS);
    } finally {
      store.close();
    }
    assertEquals("lifecycle-test-store-io", worker.get().getName());
    assertTrue(worker.get().isDaemon());
    worker.get().join(2000);
    assertFalse(worker.get().isAlive());
    verify(backend).flush("ns");
    verify(backend, never()).close();
  }

  @Test
  void ownedWorkerAlsoTerminatesAfterFlushAndBackendCloseFailures() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    IOException flushFailure = new IOException("flush failed");
    IOException closeFailure = new IOException("close failed");
    AtomicReference<Thread> worker = new AtomicReference<>();
    doAnswer(invocation -> {
      worker.set(Thread.currentThread());
      throw flushFailure;
    }).when(backend).flush("ns");
    doThrow(closeFailure).when(backend).close();
    DataStore store = DataStore.builder(backend).cacheMode(CacheMode.NONE).build();
    repository(store, "ns");
    assertSame(flushFailure, assertThrows(IOException.class, store::close));
    assertArrayEquals(new Throwable[] {closeFailure}, flushFailure.getSuppressed());
    worker.get().join(2000);
    assertFalse(worker.get().isAlive());
    assertSame(flushFailure, assertThrows(IOException.class, store::close));
    verify(backend).close();
  }

  @Test
  void originalOperationFailureIsPreservedWithSuppressedCleanupFailures() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    IOException writeFailure = new IOException("write failed");
    IOException flushFailure = new IOException("flush failed");
    IOException closeFailure = new IOException("close failed");
    doThrow(writeFailure).when(backend).save("ns", "bad", "\"value\"");
    doThrow(flushFailure).when(backend).flush("ns");
    doThrow(closeFailure).when(backend).close();
    DataStore store = rawStore(backend, Runnable::run, Duration.ofSeconds(2));
    Repository<String, String> repo = repository(store, "ns");
    ExecutionException write = assertThrows(ExecutionException.class,
        () -> repo.put("bad", "value").get(2, TimeUnit.SECONDS));
    assertSame(writeFailure, write.getCause());
    repo.put("good", "value").get(2, TimeUnit.SECONDS);
    assertSame(writeFailure, assertThrows(IOException.class, store::close));
    assertArrayEquals(new Throwable[] {flushFailure, closeFailure}, writeFailure.getSuppressed());
    assertSame(writeFailure, assertThrows(IOException.class, store::close));
    verify(backend).save("ns", "good", "\"value\"");
    verify(backend).close();
  }

  @Test
  void failedNamespaceFlushDoesNotPreventOtherNamespacesOrBackendCleanup() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    IOException failure = new IOException("namespace flush failed");
    doThrow(failure).when(backend).flush("bad");
    DataStore store = rawStore(backend, Runnable::run, Duration.ofSeconds(2));
    repository(store, "bad");
    repository(store, "good");
    assertSame(failure, assertThrows(IOException.class, store::close));
    verify(backend).flush("bad");
    verify(backend).flush("good");
    verify(backend).close();
  }

  @Test
  void suppliedExecutorServiceIsNeverShutDownOrAwaited() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ExecutorService executor = directExecutorService();
    DataStore store = rawStore(backend, executor, Duration.ofSeconds(2));
    repository(store, "ns").put("key", "value").get(2, TimeUnit.SECONDS);
    store.close();
    verify(executor, never()).shutdown();
    verify(executor, never()).shutdownNow();
    verify(executor, never()).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(backend).close();
  }

  @Test
  void executorRejectionCompletesRequestsExceptionallyAndStillReleasesIdleResources()
      throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    RejectedExecutionException rejection = new RejectedExecutionException("executor rejected");
    Executor executor = task -> {
      throw rejection;
    };
    DataStore store = rawStore(backend, executor, Duration.ofSeconds(2));
    CompletableFuture<Void> write = repository(store, "ns").put("key", "value");
    ExecutionException rejected = assertThrows(ExecutionException.class,
        () -> write.get(2, TimeUnit.SECONDS));
    assertSame(rejection, rejected.getCause());
    IOException failure = assertThrows(IOException.class, store::close);
    assertSame(rejection, failure.getCause());
    verify(backend, never()).save(anyString(), anyString(), anyString());
    verify(backend).flush("ns");
    verify(backend).close();
  }

  @Test
  void rejectionWhileOtherSubmissionsAndCloseAreQueuedLeavesNoHangingFutures() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    CountDownLatch dispatching = new CountDownLatch(1);
    CountDownLatch reject = new CountDownLatch(1);
    RejectedExecutionException rejection = new RejectedExecutionException("executor rejected");
    Executor executor = task -> {
      dispatching.countDown();
      await(reject);
      throw rejection;
    };
    DataStore store = rawStore(backend, executor, Duration.ZERO);
    Repository<String, String> repo = repository(store, "ns");
    ExecutorService caller = Executors.newSingleThreadExecutor();
    try {
      final Future<CompletableFuture<Void>> first = caller.submit(() -> repo.put("first", "one"));
      await(dispatching);
      final CompletableFuture<Void> second = repo.put("second", "two");
      assertThrows(IOException.class, store::close);
      reject.countDown();
      assertRejected(first.get(2, TimeUnit.SECONDS));
      assertRejected(second);
      assertSame(rejection, assertThrows(IOException.class, store::close).getCause());
      verify(backend, never()).save(anyString(), anyString(), anyString());
      verify(backend).close();
    } finally {
      reject.countDown();
      shutdown(caller);
    }
  }

  @Test
  void terminalRejectionStillShutsDownOwnedExecutorAfterBackendRelease() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ExecutorService owned = mock(ExecutorService.class);
    RejectedExecutionException rejection = new RejectedExecutionException("executor rejected");
    doThrow(rejection).when(owned).execute(any(Runnable.class));
    when(owned.awaitTermination(anyLong(), eq(TimeUnit.NANOSECONDS))).thenReturn(true);
    ThreadedRepositoryBuilder builder = new ThreadedRepositoryBuilder(
        backend, owned, new ObjectMapper(), true, owned, Duration.ofSeconds(2));
    DataStore store = new ConcurrentDataStore(builder);
    repository(store, "ns");
    assertSame(rejection, assertThrows(IOException.class, store::close).getCause());
    InOrder order = inOrder(backend, owned);
    order.verify(backend).flush("ns");
    order.verify(backend).close();
    order.verify(owned).shutdown();
    verify(owned, never()).shutdownNow();
  }

  @Test
  void timeoutAwaitingOwnedTerminationReportsIncompleteReleaseAndPreservesFlushFailure()
      throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    IOException flushFailure = new IOException("flush failed");
    doThrow(flushFailure).when(backend).flush("ns");
    ExecutorService owned = directExecutorService();
    when(owned.awaitTermination(anyLong(), eq(TimeUnit.NANOSECONDS))).thenReturn(false, true);
    DataStore store = new ConcurrentDataStore(new ThreadedRepositoryBuilder(
        backend, owned, new ObjectMapper(), true, owned, Duration.ZERO));
    repository(store, "ns");

    IOException timeout = assertThrows(IOException.class, store::close);
    assertInstanceOf(TimeoutException.class, timeout.getCause());
    assertArrayEquals(new Throwable[] {flushFailure}, timeout.getSuppressed());
    assertSame(flushFailure, assertThrows(IOException.class, store::close));
    verify(backend).close();
    verify(owned).shutdown();
    verify(owned, never()).shutdownNow();
  }

  @Test
  void interruptionAwaitingOwnedTerminationPreservesInterruptWithoutRepeatingCleanup()
      throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    ExecutorService owned = directExecutorService();
    when(owned.awaitTermination(anyLong(), eq(TimeUnit.NANOSECONDS)))
        .thenThrow(new InterruptedException("interrupted termination wait")).thenReturn(true);
    DataStore store = new ConcurrentDataStore(new ThreadedRepositoryBuilder(
        backend, owned, new ObjectMapper(), true, owned, Duration.ofSeconds(2)));
    try {
      IOException interrupted = assertThrows(IOException.class, store::close);
      assertInstanceOf(InterruptedException.class, interrupted.getCause());
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
    store.close();
    verify(backend).close();
    verify(owned).shutdown();
    verify(owned, never()).shutdownNow();
  }

  @Test
  void runtimeCleanupFailuresDoNotMaskOriginalFailureOrSkipOwnedShutdown() throws Exception {
    StorageBackend backend = mock(StorageBackend.class);
    IOException flushFailure = new IOException("flush failed");
    doThrow(flushFailure).when(backend).flush("ns");
    IllegalStateException closeFailure = new IllegalStateException("backend close failed");
    doThrow(closeFailure).when(backend).close();
    ExecutorService owned = directExecutorService();
    IllegalStateException shutdownFailure = new IllegalStateException("executor shutdown failed");
    doThrow(shutdownFailure).when(owned).shutdown();
    when(owned.awaitTermination(anyLong(), eq(TimeUnit.NANOSECONDS))).thenReturn(true);
    DataStore store = new ConcurrentDataStore(new ThreadedRepositoryBuilder(
        backend, owned, new ObjectMapper(), true, owned, Duration.ofSeconds(2)));
    repository(store, "ns");
    assertSame(flushFailure, assertThrows(IOException.class, store::close));
    assertArrayEquals(new Throwable[] {closeFailure, shutdownFailure},
        flushFailure.getSuppressed());
    verify(owned).shutdown();
    verify(owned, never()).shutdownNow();
  }

  @Test
  void closeTimeoutRejectsNullNegativeAndOverflowingDurations() {
    DataStoreBuilder builder = DataStore.builder(mock(StorageBackend.class));
    assertThrows(NullPointerException.class, () -> builder.closeTimeout(null));
    assertThrows(IllegalArgumentException.class,
        () -> builder.closeTimeout(Duration.ofNanos(-1)));
    assertThrows(IllegalArgumentException.class,
        () -> builder.closeTimeout(Duration.ofSeconds(Long.MAX_VALUE)));
    assertSame(builder, builder.closeTimeout(Duration.ZERO));
    assertSame(builder, builder.closeTimeout(Duration.ofSeconds(30)));
  }

  private static DataStore rawStore(StorageBackend backend, Executor executor, Duration timeout) {
    return DataStore.builder(backend).cacheMode(CacheMode.NONE)
        .executor(executor).closeTimeout(timeout).build();
  }

  private static Repository<String, String> repository(DataStore store, String namespace) {
    return store.repository(namespace, String.class, KeySerializers.forString());
  }

  private static ExecutorService directExecutorService() {
    ExecutorService executor = mock(ExecutorService.class);
    doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(executor).execute(any(Runnable.class));
    return executor;
  }

  private static void assertRejected(CompletableFuture<?> result) {
    ExecutionException failure = assertThrows(ExecutionException.class,
        () -> result.get(2, TimeUnit.SECONDS));
    assertInstanceOf(RejectedExecutionException.class, failure.getCause());
  }

  private static void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(2, TimeUnit.SECONDS), "Latch was not released");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for test latch", e);
    }
  }

  private static void shutdown(ExecutorService executor) throws InterruptedException {
    executor.shutdown();
    assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS), "Test executor did not terminate");
  }

  private static final class ControlledExecutor implements Executor {
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    @Override
    public void execute(Runnable command) {
      tasks.add(command);
    }

    int pending() {
      return tasks.size();
    }

    void runNext() throws InterruptedException {
      Runnable task = tasks.poll(2, TimeUnit.SECONDS);
      assertNotNull(task, "No store drain was scheduled");
      task.run();
    }
  }
}
