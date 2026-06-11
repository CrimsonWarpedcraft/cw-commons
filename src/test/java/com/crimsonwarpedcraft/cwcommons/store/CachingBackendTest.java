package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
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
  void saveWithWriteThroughAtomicWritesToDelegate() throws IOException {
    CachingBackend writeThroughBackend =
        new CachingBackend(mockDelegate, WritePolicy.WRITE_THROUGH_ATOMIC);
    writeThroughBackend.save("ns", "key", "val");
    verify(mockDelegate).save("ns", "key", "val");
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
  }

  @Test
  void deleteRemovesFromCacheAndCallsDelegate() throws IOException {
    backend.save("ns", "key", "val");
    backend.delete("ns", "key");
    verify(mockDelegate).delete("ns", "key");
    assertFalse(backend.load("ns", "key").isPresent());
    verify(mockDelegate).load("ns", "key");
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
}
