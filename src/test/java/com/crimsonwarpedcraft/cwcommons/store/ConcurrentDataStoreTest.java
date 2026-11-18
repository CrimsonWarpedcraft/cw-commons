package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConcurrentDataStoreTest {

  private StorageBackend mockBackend;
  private ConcurrentDataStore store;
  private Repository<String, String> repo;

  @BeforeEach
  void setUp() throws IOException {
    mockBackend = mock(StorageBackend.class);
    when(mockBackend.load(any(), any())).thenReturn(Optional.empty());
    when(mockBackend.loadAll(any())).thenReturn(new HashMap<>());
    store = new ConcurrentDataStore(
        new ThreadedRepositoryBuilder(
            new CachingBackend(mockBackend, WritePolicy.CACHE_AND_FLUSH),
            Runnable::run,
            new ObjectMapper()));
    repo = store.repository("ns", String.class, KeySerializers.forString());
  }

  @Test
  void nullRepositoryBuilderThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new ConcurrentDataStore(null));
  }

  @Test
  void nullExecutorThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> new ThreadedRepositoryBuilder(
            new CachingBackend(mockBackend, WritePolicy.CACHE_AND_FLUSH), null,
            new ObjectMapper()));
  }

  @Test
  void nullMapperThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> new ThreadedRepositoryBuilder(
            new CachingBackend(mockBackend, WritePolicy.CACHE_AND_FLUSH), Runnable::run, null));
  }

  @Test
  void repositoryReturnsNonNull() {
    assertNotNull(repo);
  }

  @Test
  void sameNamespaceReturnsSameInstance() {
    assertSame(repo, store.repository("ns", String.class, KeySerializers.forString()));
  }

  @Test
  void differentNamespacesReturnDifferentInstances() {
    Repository<String, String> r2 =
        store.repository("ns2", String.class, KeySerializers.forString());
    assertNotSame(repo, r2);
  }

  @Test
  void nullNamespaceThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> store.repository(null, String.class, KeySerializers.forString()));
  }

  @Test
  void nullTypeThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> store.repository("ns", null, KeySerializers.forString()));
  }

  @Test
  void nullKeySerializerThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> store.repository("ns", String.class, null));
  }

  @Test
  void putAndGetRoundtrip() throws Exception {
    repo.put("key", "hello").get();

    Optional<String> result = repo.get("key").get();

    assertTrue(result.isPresent());
    assertEquals("hello", result.get());
  }

  @Test
  void cacheAndFlushPolicyBuffersUntilFlush() throws Exception {
    repo.put("key", "val").get();
    verify(mockBackend, never()).save(any(), any(), any());

    store.flush().get();
    verify(mockBackend).save(eq("ns"), eq("key"), any());
  }

  @Test
  void writeThroughPolicyWritesImmediately() throws Exception {
    StorageBackend writeThroughMock = mock(StorageBackend.class);
    when(writeThroughMock.load(any(), any())).thenReturn(Optional.empty());
    when(writeThroughMock.loadAll(any())).thenReturn(new HashMap<>());
    try (ConcurrentDataStore writeThroughStore = new ConcurrentDataStore(
        new ThreadedRepositoryBuilder(
            new CachingBackend(writeThroughMock, WritePolicy.WRITE_THROUGH_ATOMIC),
            Runnable::run,
            new ObjectMapper()))) {
      Repository<String, String> wtRepo =
          writeThroughStore.repository("ns", String.class, KeySerializers.forString());
      wtRepo.put("key", "val").get();
      verify(writeThroughMock).save(eq("ns"), eq("key"), any());
    }
  }

  @Test
  void flushPropagatesWritesToBackend() throws Exception {
    repo.put("key", "value").get();
    store.flush().get();
    verify(mockBackend).save(eq("ns"), eq("key"), any());
  }

  @Test
  void closeFlushesData() throws Exception {
    repo.put("key", "value").get();
    store.close();
    verify(mockBackend).save(any(), any(), any());
  }

  @Test
  void closeDoesNotCloseNonOwnedBackend() throws Exception {
    store.close();
    verify(mockBackend, never()).close();
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

  @Test
  void getAllReturnsEmptyWhenNoEntries() throws Exception {
    assertTrue(repo.getAll().get().isEmpty());
  }

  @Test
  void getAllReturnsPutEntries() throws Exception {
    repo.put("a", "alpha").get();
    repo.put("b", "beta").get();

    Map<String, String> result = repo.getAll().get();

    assertEquals(2, result.size());
    assertEquals("alpha", result.get("a"));
    assertEquals("beta", result.get("b"));
  }

  @Test
  void deleteRemovesEntry() throws Exception {
    repo.put("key", "val").get();
    repo.delete("key").get();

    assertFalse(repo.get("key").get().isPresent());
  }

  @Test
  void throwingTransformCannotChangeEvenMutableCachedData() throws IOException {
    var arrays = store.repository("arrays", int[].class, KeySerializers.forString());
    arrays.put("key", new int[] {7}).join();
    var failure = new IllegalArgumentException("bad transform");
    var update = arrays.update("key", current -> {
      current.orElseThrow()[0]++;
      throw failure;
    });
    assertSame(failure, assertThrows(CompletionException.class, update::join).getCause());
    assertEquals(7, arrays.get("key").join().orElseThrow()[0]);
    assertSame(failure, assertThrows(IOException.class, store::close).getCause());
    verify(mockBackend).save("arrays", "key", "[7]");
  }

  @Test
  void nullResultIsNotDeleteOrInsert() throws IOException {
    var update = repo.update("absent", current -> null);
    assertInstanceOf(NullPointerException.class,
        assertThrows(CompletionException.class, update::join).getCause());
    assertTrue(repo.get("absent").join().isEmpty());
    assertThrows(IOException.class, store::close);
    verify(mockBackend, never()).save(any(), any(), any());
    verify(mockBackend, never()).delete(any(), any());
  }

  @Test
  void serializationFailureDoesNotBeginSave() throws IOException {
    when(mockBackend.load("ns", "key")).thenReturn(Optional.of("\"old\""));
    var mapper = spy(new ObjectMapper());
    var failure = JsonMappingException.fromUnexpectedIOE(new IOException("bad encoding"));
    doThrow(failure).when(mapper).writeValueAsString("new");
    DataStore raw = DataStore.builder(mockBackend).executor(Runnable::run)
        .cacheMode(CacheMode.NONE).mapper(mapper).build();
    var values = raw.repository("ns", String.class, KeySerializers.forString());
    var update = values.update("key", current -> "new");
    assertSame(failure, assertThrows(CompletionException.class, update::join).getCause());
    assertEquals(Optional.of("old"), values.get("key").join());
    assertSame(failure, assertThrows(IOException.class, raw::close));
    verify(mockBackend, never()).save(any(), any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void customRepositoryWithoutUpdateFailsExplicitly() {
    Repository<String, String> custom = mock(Repository.class, CALLS_REAL_METHODS);
    var update = custom.update("key", current -> "new");
    assertInstanceOf(UnsupportedOperationException.class,
        assertThrows(CompletionException.class, update::join).getCause());
    verify(custom, never()).get(any());
    verify(custom, never()).put(any(), any());
  }
}
