package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteBackendTest {

  private SqliteBackend backend;

  @BeforeEach
  void setUp() throws IOException {
    backend = new SqliteBackend("jdbc:sqlite::memory:");
  }

  @AfterEach
  void tearDown() throws IOException {
    if (backend != null) {
      backend.close();
    }
  }

  @Test
  void saveAndLoadRoundtrip() throws IOException {
    backend.save("ns", "k1", "{\"v\":1}");
    Optional<String> result = backend.load("ns", "k1");
    assertTrue(result.isPresent());
    assertEquals("{\"v\":1}", result.get());
  }

  @Test
  void loadReturnsEmptyForMissingKey() throws IOException {
    assertFalse(backend.load("ns", "absent").isPresent());
  }

  @Test
  void loadAllReturnsNamespacedEntries() throws IOException {
    backend.save("ns", "a", "1");
    backend.save("ns", "b", "2");
    backend.save("other", "a", "99");
    Map<String, String> result = backend.loadAll("ns");
    assertEquals(2, result.size());
    assertEquals("1", result.get("a"));
    assertEquals("2", result.get("b"));
  }

  @Test
  void saveReplacesExistingValue() throws IOException {
    backend.save("ns", "key", "old");
    backend.save("ns", "key", "new");
    assertEquals("new", backend.load("ns", "key").orElseThrow());
  }

  @Test
  void deleteRemovesEntry() throws IOException {
    backend.save("ns", "key", "val");
    backend.delete("ns", "key");
    assertFalse(backend.load("ns", "key").isPresent());
  }

  @Test
  void deleteNonExistentKeyIsIdempotent() throws IOException {
    backend.delete("ns", "nonexistent");
  }

  @Test
  void createsMissingParentDirectories(@TempDir Path tempDir) throws IOException {
    File nested = new File(tempDir.resolve("sub").toFile(), "nested.db");
    try (SqliteBackend nestedBackend = new SqliteBackend(nested)) {
      nestedBackend.save("ns", "k", "v");
      assertTrue(nestedBackend.load("ns", "k").isPresent());
    }
  }

  @Test
  void getLocalDataStoreClosesBackend(@TempDir Path tempDir) throws Exception {
    try (DataStore store = DataStore.getLocalDataStore("test", tempDir.toFile())) {
      assertNotNull(store.repository("ns", String.class, KeySerializers.forString()));
    }
    // store.close() propagates through ThreadedRepositoryBuilder → CachingBackend → SqliteBackend,
    // releasing the file lock so @TempDir cleanup succeeds on Windows.
  }
}
