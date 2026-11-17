package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachingBackendTest {

  private StorageBackend mockDelegate;
  private CachingBackend backend;

  @BeforeEach
  void setUp() throws IOException {
    mockDelegate = mock(StorageBackend.class);
    when(mockDelegate.load(any(), any())).thenReturn(Optional.empty());
    when(mockDelegate.loadAll(any())).thenReturn(new HashMap<>());
    backend = new CachingBackend(mockDelegate, WritePolicy.CACHE_AND_FLUSH);
  }

  @Test
  void nullDelegateThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> new CachingBackend(null, WritePolicy.CACHE_AND_FLUSH));
  }

  @Test
  void nullWritePolicyThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new CachingBackend(mockDelegate, null));
  }

  @Test
  void saveWithCacheAndFlushDoesNotWriteThrough() throws IOException {
    backend.save("ns", "key", "val");
    verify(mockDelegate, never()).save(any(), any(), any());
  }

  @Test
  void saveWithWriteThroughAtomicWritesToDelegateAndCachesCleanValue() throws IOException {
    CachingBackend writeThroughBackend =
        new CachingBackend(mockDelegate, WritePolicy.WRITE_THROUGH_ATOMIC);
    writeThroughBackend.save("ns", "key", "val");
    assertEquals(Optional.of("val"), writeThroughBackend.load("ns", "key"));
    writeThroughBackend.flush("ns");
    verify(mockDelegate).save("ns", "key", "val");
    verify(mockDelegate, never()).load(any(), any());
    verify(mockDelegate).flush("ns");
  }

  @Test
  void failedNewWriteThroughSaveDoesNotCacheValueAndCanBeRetried() throws IOException {
    CachingBackend writeThroughBackend =
        new CachingBackend(mockDelegate, WritePolicy.WRITE_THROUGH_ATOMIC);
    IOException failure = new IOException("save failed");
    doThrow(failure).when(mockDelegate).save("ns", "key", "val");

    assertSame(failure,
        assertThrows(IOException.class, () -> writeThroughBackend.save("ns", "key", "val")));
    assertEquals(Optional.empty(), writeThroughBackend.load("ns", "key"));
    assertTrue(writeThroughBackend.loadAll("ns").isEmpty());
    writeThroughBackend.flush("ns");
    verify(mockDelegate).save("ns", "key", "val");

    doNothing().when(mockDelegate).save("ns", "key", "val");
    writeThroughBackend.save("ns", "key", "val");
    assertEquals(Optional.of("val"), writeThroughBackend.load("ns", "key"));
  }

  @Test
  void failedWriteThroughReplacementPreservesPreviouslySavedValue() throws IOException {
    CachingBackend writeThroughBackend =
        new CachingBackend(mockDelegate, WritePolicy.WRITE_THROUGH_ATOMIC);
    writeThroughBackend.save("ns", "key", "old");
    doThrow(new IOException("save failed")).when(mockDelegate).save("ns", "key", "new");

    assertThrows(IOException.class, () -> writeThroughBackend.save("ns", "key", "new"));
    assertEquals(Optional.of("old"), writeThroughBackend.load("ns", "key"));
    assertEquals(Map.of("key", "old"), writeThroughBackend.loadAll("ns"));
    writeThroughBackend.flush("ns");
    verify(mockDelegate).save("ns", "key", "old");
    verify(mockDelegate).save("ns", "key", "new");
    verify(mockDelegate, never()).load(any(), any());
  }

  @Test
  void failedWriteThroughReplacementPreservesPreviouslyLoadedValue() throws IOException {
    CachingBackend writeThroughBackend =
        new CachingBackend(mockDelegate, WritePolicy.WRITE_THROUGH_ATOMIC);
    when(mockDelegate.load("ns", "key")).thenReturn(Optional.of("old"));
    assertEquals(Optional.of("old"), writeThroughBackend.load("ns", "key"));
    doThrow(new IOException("save failed")).when(mockDelegate).save("ns", "key", "new");

    assertThrows(IOException.class, () -> writeThroughBackend.save("ns", "key", "new"));
    assertEquals(Optional.of("old"), writeThroughBackend.load("ns", "key"));
    verify(mockDelegate).load("ns", "key");
  }

  @Test
  void loadReturnsCachedValueWithoutDelegateCall() throws IOException {
    backend.save("ns", "key", "val");
    Optional<String> result = backend.load("ns", "key");
    assertTrue(result.isPresent());
    assertEquals("val", result.get());
    verify(mockDelegate, never()).load(any(), any());
  }

  @Test
  void loadMissLoadsDelegateAndPopulatesCache() throws IOException {
    when(mockDelegate.load("ns", "key")).thenReturn(Optional.of("val"));
    Optional<String> first = backend.load("ns", "key");
    Optional<String> second = backend.load("ns", "key");
    assertTrue(first.isPresent());
    assertEquals("val", first.get());
    assertEquals(first, second);
    verify(mockDelegate, times(1)).load("ns", "key");
  }

  @Test
  void flushSavesDirtyEntriesAndMarksThemClean() throws IOException {
    backend.save("ns", "key", "val");
    backend.flush("ns");
    verify(mockDelegate).save("ns", "key", "val");
    backend.flush("ns");
    verify(mockDelegate, times(1)).save(any(), any(), any());
    verify(mockDelegate, times(2)).flush("ns");
  }

  @Test
  void partialFlushFailuresRetainAllDirtyEntriesForRepeatedRetries() throws IOException {
    backend.save("ns", "first", "one");
    backend.save("ns", "second", "two");
    List<String> savedKeys = new ArrayList<>();
    doAnswer(invocation -> {
      savedKeys.add(invocation.getArgument(1));
      if (savedKeys.size() == 2 || savedKeys.size() == 4) {
        throw new IOException("partial save failure");
      }
      return null;
    }).when(mockDelegate).save(any(), any(), any());

    assertThrows(IOException.class, () -> backend.flush("ns"));
    assertThrows(IOException.class, () -> backend.flush("ns"));
    assertEquals(Map.of("first", "one", "second", "two"), backend.loadAll("ns"));
    verify(mockDelegate, never()).flush(any());

    backend.flush("ns");
    backend.flush("ns");
    verify(mockDelegate, times(3)).save("ns", "first", "one");
    verify(mockDelegate, times(3)).save("ns", "second", "two");
    verify(mockDelegate, times(2)).flush("ns");
  }

  @Test
  void flushWithoutLocalCacheForwardsDelegateFailureAndRetry() throws IOException {
    IOException failure = new IOException("flush failed");
    doThrow(failure).doNothing().when(mockDelegate).flush("uncached");

    assertSame(failure, assertThrows(IOException.class, () -> backend.flush("uncached")));
    backend.flush("uncached");
    verify(mockDelegate, times(2)).flush("uncached");
    verify(mockDelegate, never()).save(any(), any(), any());
  }

  @Test
  void flushOnlySavesDirtyEntriesInRequestedNamespace() throws IOException {
    backend.save("ns", "key", "val");
    backend.save("other", "key", "otherVal");
    backend.flush("ns");
    verify(mockDelegate).save("ns", "key", "val");
    verify(mockDelegate, never()).save("other", "key", "otherVal");
    backend.flush("other");
    verify(mockDelegate).save("other", "key", "otherVal");
  }

  @Test
  void flushCommitsCustomBufferingDelegateEvenWithoutLocalCache() throws IOException {
    BufferingBackend delegate = new BufferingBackend();
    CachingBackend cachingBackend = new CachingBackend(delegate, WritePolicy.CACHE_AND_FLUSH);
    delegate.save("uncached", "key", "external");
    cachingBackend.flush("uncached");
    assertEquals(Optional.of("external"), delegate.load("uncached", "key"));

    cachingBackend.save("ns", "key", "val");
    assertTrue(delegate.load("ns", "key").isEmpty());
    cachingBackend.flush("ns");
    assertEquals(Optional.of("val"), delegate.load("ns", "key"));
  }

  @Test
  void downstreamFlushFailuresRetainDirtyValuesUntilSuccessfulRetry() throws IOException {
    BufferingBackend delegate = new BufferingBackend();
    CachingBackend cachingBackend = new CachingBackend(delegate, WritePolicy.CACHE_AND_FLUSH);
    cachingBackend.save("ns", "key", "val");
    delegate.failFlush = true;

    assertThrows(IOException.class, () -> cachingBackend.flush("ns"));
    assertThrows(IOException.class, () -> cachingBackend.flush("ns"));
    assertTrue(delegate.load("ns", "key").isEmpty());
    assertEquals(Optional.of("val"), cachingBackend.load("ns", "key"));

    delegate.failFlush = false;
    cachingBackend.flush("ns");
    assertEquals(Optional.of("val"), delegate.load("ns", "key"));
    cachingBackend.flush("ns");
    assertEquals(Optional.of("val"), delegate.load("ns", "key"));
  }

  @Test
  void deleteRemovesFromCacheAndCallsDelegate() throws IOException {
    backend.save("ns", "key", "val");
    backend.delete("ns", "key");
    verify(mockDelegate).delete("ns", "key");
    assertFalse(backend.load("ns", "key").isPresent());
    verify(mockDelegate).load("ns", "key");
    backend.flush("ns");
    backend.flush("ns");
    verify(mockDelegate, never()).save(any(), any(), any());
  }

  @Test
  void failedDeletePreservesBufferedValueForFlush() throws IOException {
    backend.save("ns", "key", "val");
    IOException failure = new IOException("delete failed");
    doThrow(failure).when(mockDelegate).delete("ns", "key");

    assertSame(failure, assertThrows(IOException.class, () -> backend.delete("ns", "key")));
    assertEquals(Optional.of("val"), backend.load("ns", "key"));
    assertEquals(Map.of("key", "val"), backend.loadAll("ns"));
    backend.flush("ns");
    backend.flush("ns");
    verify(mockDelegate).save("ns", "key", "val");
    verify(mockDelegate, never()).load(any(), any());
  }

  @Test
  void failedDeletePreservesCleanCachedValueWithoutMakingItDirty() throws IOException {
    when(mockDelegate.load("ns", "key")).thenReturn(Optional.of("val"));
    assertEquals(Optional.of("val"), backend.load("ns", "key"));
    doThrow(new IOException("delete failed")).when(mockDelegate).delete("ns", "key");

    assertThrows(IOException.class, () -> backend.delete("ns", "key"));
    assertEquals(Optional.of("val"), backend.load("ns", "key"));
    backend.flush("ns");
    verify(mockDelegate, never()).save(any(), any(), any());
    verify(mockDelegate).load("ns", "key");
  }

  @Test
  void successfulDeleteAfterFailedFlushDoesNotResurrectValue() throws IOException {
    backend.save("ns", "key", "val");
    doThrow(new IOException("flush failed")).doNothing().when(mockDelegate).flush("ns");
    assertThrows(IOException.class, () -> backend.flush("ns"));

    backend.delete("ns", "key");
    backend.flush("ns");
    assertEquals(Optional.empty(), backend.load("ns", "key"));
    assertTrue(backend.loadAll("ns").isEmpty());
    verify(mockDelegate).delete("ns", "key");
    verify(mockDelegate).save("ns", "key", "val");
  }

  @Test
  void loadAllMergesCacheAndDelegateEntries() throws IOException {
    when(mockDelegate.loadAll("ns")).thenReturn(Map.of("fromDelegate", "delegateVal"));
    backend.save("ns", "fromCache", "cacheVal");
    Map<String, String> result = backend.loadAll("ns");
    assertEquals(2, result.size());
    assertEquals("cacheVal", result.get("fromCache"));
    assertEquals("delegateVal", result.get("fromDelegate"));
  }

  @Test
  void closeCallsDelegateClose() throws IOException {
    backend.close();
    verify(mockDelegate).close();
  }

  @Test
  void closePropagatesDelegateFailure() throws IOException {
    IOException failure = new IOException("close failed");
    doThrow(failure).when(mockDelegate).close();
    assertSame(failure, assertThrows(IOException.class, backend::close));
  }

  private static final class BufferingBackend implements StorageBackend {

    private final Map<String, Map<String, String>> pending = new HashMap<>();
    private final Map<String, Map<String, String>> persisted = new HashMap<>();
    private boolean failFlush;

    @Override
    public void save(String namespace, String key, String json) {
      pending.computeIfAbsent(namespace, _ -> new HashMap<>()).put(key, json);
    }

    @Override
    public Optional<String> load(String namespace, String key) {
      return Optional.ofNullable(persisted.getOrDefault(namespace, Map.of()).get(key));
    }

    @Override
    public Map<String, String> loadAll(String namespace) {
      return new HashMap<>(persisted.getOrDefault(namespace, Map.of()));
    }

    @Override
    public void delete(String namespace, String key) {
      Map<String, String> buffered = pending.get(namespace);
      if (buffered != null) {
        buffered.remove(key);
      }
      Map<String, String> stored = persisted.get(namespace);
      if (stored != null) {
        stored.remove(key);
      }
    }

    @Override
    public void flush(String namespace) throws IOException {
      if (failFlush) {
        // A failed commit may discard the delegate's uncommitted batch.
        pending.remove(namespace);
        throw new IOException("downstream flush failed");
      }
      persisted.computeIfAbsent(namespace, _ -> new HashMap<>())
          .putAll(pending.getOrDefault(namespace, Map.of()));
      pending.remove(namespace);
    }

    @Override
    public void close() {}
  }
}
