package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThreadedRepositoryTest {

  private StorageBackend mockBackend;
  private ThreadedRepository<String, String> repo;

  @BeforeEach
  void setUp() throws IOException {
    mockBackend = mock(StorageBackend.class);
    when(mockBackend.load(any(), any())).thenReturn(Optional.empty());
    when(mockBackend.loadAll(any())).thenReturn(new HashMap<>());
    repo = new ThreadedRepository<>(
        "ns", String.class, KeySerializers.forString(),
        mockBackend, Runnable::run, new ObjectMapper());
  }

  @Test
  void putCallsBackendSaveWithSerializedValue() throws Exception {
    repo.put("key", "hello").get();
    verify(mockBackend).save("ns", "key", "\"hello\"");
  }

  @Test
  void getReturnsDeserializedValue() throws Exception {
    when(mockBackend.load("ns", "key")).thenReturn(Optional.of("\"hello\""));

    Optional<String> result = repo.get("key").get();

    assertTrue(result.isPresent());
    assertEquals("hello", result.get());
  }

  @Test
  void getReturnsEmptyForMissingKey() throws Exception {
    assertFalse(repo.get("key").get().isPresent());
  }

  @Test
  void getAllReturnsDeserializedEntries() throws Exception {
    when(mockBackend.loadAll("ns")).thenReturn(Map.of("k", "\"v\""));

    Map<String, String> result = repo.getAll().get();

    assertEquals(1, result.size());
    assertEquals("v", result.get("k"));
  }

  @Test
  void getAllReturnsEmptyWhenNoEntries() throws Exception {
    assertTrue(repo.getAll().get().isEmpty());
  }

  @Test
  void deleteCallsBackendDelete() throws Exception {
    repo.delete("key").get();
    verify(mockBackend).delete("ns", "key");
  }

  @Test
  void flushCallsBackendFlush() throws Exception {
    repo.flush().get();
    verify(mockBackend).flush("ns");
  }

  @Test
  void getNullKeyThrowsNpe() {
    assertThrows(NullPointerException.class, () -> repo.get(null));
  }

  @Test
  void putNullKeyThrowsNpe() {
    assertThrows(NullPointerException.class, () -> repo.put(null, "val"));
  }

  @Test
  void putNullValueThrowsNpe() {
    assertThrows(NullPointerException.class, () -> repo.put("key", null));
  }

  @Test
  void deleteNullKeyThrowsNpe() {
    assertThrows(NullPointerException.class, () -> repo.delete(null));
  }
}
