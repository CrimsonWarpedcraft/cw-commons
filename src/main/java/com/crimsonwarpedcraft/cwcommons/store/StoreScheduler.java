package com.crimsonwarpedcraft.cwcommons.store;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** One admission gate and one logical I/O lane, independent of the delegate's worker count. */
final class StoreScheduler {

  private enum State { OPEN, CLOSING, CLOSED }

  private record Task(Runnable action, Consumer<RejectedExecutionException> reject) {}

  private final Object lock = new Object();
  private final Executor executor;
  private final ExecutorService ownedExecutor;
  private final long closeWaitNanos;
  private final Queue<Task> tasks = new ArrayDeque<>();
  private final ThreadLocal<Boolean> executing = ThreadLocal.withInitial(() -> false);
  private final CompletableFuture<Void> closed = new CompletableFuture<>();
  private State state = State.OPEN;
  private boolean running;
  private Throwable failure;

  StoreScheduler(Executor executor, ExecutorService ownedExecutor, Duration closeTimeout) {
    this.executor = executor;
    this.ownedExecutor = ownedExecutor;
    this.closeWaitNanos = closeTimeout.toNanos();
  }

  <T> T whileOpen(Supplier<T> action) {
    synchronized (lock) {
      if (state != State.OPEN) {
        throw closedRejection();
      }
      return action.get();
    }
  }

  <T> CompletableFuture<T> submit(Callable<T> action) {
    CompletableFuture<T> result = new CompletableFuture<>();
    boolean launch;
    synchronized (lock) {
      if (state != State.OPEN) {
        return CompletableFuture.failedFuture(closedRejection());
      }
      tasks.add(new Task(() -> {
        try {
          result.complete(action.call());
        } catch (Throwable e) {
          recordFailure(e);
          result.completeExceptionally(e);
        }
      }, result::completeExceptionally));
      launch = !running;
      running = true;
    }
    if (launch) {
      launch();
    }
    return result;
  }

  void close(Supplier<List<Callable<Void>>> cleanup) throws IOException {
    if (executing.get()) {
      throw new IOException("Cannot close store from its own execution context; "
          + "call close from another thread");
    }
    long started = System.nanoTime();
    boolean launch = false;
    synchronized (lock) {
      if (state == State.OPEN) {
        List<Callable<Void>> finalTasks = cleanup.get();
        state = State.CLOSING;
        tasks.add(new Task(() -> finish(finalTasks), ignored -> finish(finalTasks)));
        launch = !running;
        running = true;
      }
    }
    if (launch) {
      launch();
    }
    awaitClose(started);
  }

  private void launch() {
    try {
      // A single drain owns the lane until it is empty; no per-operation rescheduling race.
      executor.execute(() -> drain(null));
    } catch (RejectedExecutionException e) {
      recordFailure(e);
      // No drain started: fail each affected future explicitly, then release resources only once
      // the lane is idle. Terminal cleanup can safely run on this caller, but writes cannot.
      drain(e);
    }
  }

  private void drain(RejectedExecutionException rejection) {
    executing.set(true);
    try {
      while (true) {
        Task task;
        synchronized (lock) {
          task = tasks.poll();
          if (task == null) {
            running = false;
            return;
          }
        }
        if (rejection == null) {
          task.action().run();
        } else {
          task.reject().accept(rejection);
        }
      }
    } finally {
      executing.remove();
    }
  }

  private void finish(List<Callable<Void>> cleanup) {
    for (Callable<Void> action : cleanup) {
      try {
        action.call();
      } catch (Throwable e) {
        recordFailure(e);
      }
    }
    if (ownedExecutor != null) {
      try {
        ownedExecutor.shutdown();
      } catch (Throwable e) {
        recordFailure(e);
      }
    }
    Throwable cause;
    synchronized (lock) {
      state = State.CLOSED;
      cause = failure;
    }
    if (cause == null) {
      closed.complete(null);
    } else {
      closed.completeExceptionally(cause);
    }
  }

  private void awaitClose(long started) throws IOException {
    IOException closeFailure = null;
    try {
      try {
        closed.get(remainingNanos(started), TimeUnit.NANOSECONDS);
      } catch (ExecutionException e) {
        closeFailure = asIoException(e.getCause());
      }
      if (ownedExecutor != null
          && !ownedExecutor.awaitTermination(remainingNanos(started), TimeUnit.NANOSECONDS)) {
        throw new TimeoutException("Owned store executor has not terminated");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw incompleteClose("interrupted", e, closeFailure);
    } catch (TimeoutException e) {
      throw incompleteClose("timed out", e, closeFailure);
    }
    if (closeFailure != null) {
      throw closeFailure;
    }
  }

  private long remainingNanos(long started) {
    return Math.max(0, closeWaitNanos - (System.nanoTime() - started));
  }

  private IOException incompleteClose(String reason, Exception cause, IOException closeFailure) {
    IOException result = new IOException("Store close " + reason
        + "; persistence or resource release is incomplete. Cleanup is not cancelled; "
        + "no active I/O or queued writes were cancelled", cause);
    synchronized (lock) {
      if (closeFailure != null) {
        result.addSuppressed(closeFailure);
      } else if (failure != null) {
        result.addSuppressed(failure);
      }
    }
    return result;
  }

  private void recordFailure(Throwable cause) {
    synchronized (lock) {
      if (failure == null) {
        failure = cause;
      } else if (failure != cause) {
        failure.addSuppressed(cause);
      }
    }
  }

  private static IOException asIoException(Throwable cause) {
    return cause instanceof IOException io ? io : new IOException("Store close failed", cause);
  }

  private static RejectedExecutionException closedRejection() {
    return new RejectedExecutionException("Store is closing or closed");
  }
}
