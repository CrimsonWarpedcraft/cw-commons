package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataStoreBuilderTest {

  private StorageBackend backend;

  @BeforeEach
  void setUp() throws IOException {
    backend = mock(StorageBackend.class);
    when(backend.load(any(), any())).thenReturn(Optional.empty());
    when(backend.loadAll(any())).thenReturn(new HashMap<>());
  }

  @Test
  void nullBackendThrowsNpe() {
    assertThrows(NullPointerException.class, () -> DataStore.builder(null));
  }

  @Test
  void buildsWorkingStore() throws Exception {
    try (DataStore store = DataStore.builder(backend).executor(Runnable::run).build()) {
      Repository<String, String> repo =
          store.repository("ns", String.class, KeySerializers.forString());
      repo.put("k", "v").get();
      assertEquals("v", repo.get("k").get().orElseThrow());
    }
  }

  @Test
  void closeBackendTrueClosesBackend() throws Exception {
    try (DataStore store =
        DataStore.builder(backend).executor(Runnable::run).closeBackend(true).build()) {
      store.repository("ns", String.class, KeySerializers.forString());
    }
    verify(backend).close();
  }

  @Test
  void closeBackendFalseRetainsBackend() throws Exception {
    DataStore store =
        DataStore.builder(backend).executor(Runnable::run).closeBackend(false).build();
    store.close();
    verify(backend, never()).close();
  }

  @Test
  void writeThroughPolicyWritesImmediately() throws Exception {
    try (DataStore store = DataStore.builder(backend)
        .executor(Runnable::run)
        .writePolicy(WritePolicy.WRITE_THROUGH_ATOMIC)
        .build()) {
      Repository<String, String> repo =
          store.repository("ns", String.class, KeySerializers.forString());
      repo.put("k", "v").get();
      verify(backend).save(eq("ns"), eq("k"), any());
    }
  }
}
