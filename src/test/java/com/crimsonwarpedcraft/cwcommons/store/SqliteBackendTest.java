package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
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
  @SuppressWarnings("removal") // intentionally exercises the deprecated 2-arg factory
  void getLocalDataStoreClosesBackend(@TempDir Path tempDir) throws Exception {
    try (DataStore store = DataStore.getLocalDataStore("test", tempDir.toFile())) {
      assertNotNull(store.repository("ns", String.class, KeySerializers.forString()));
    }
    // store.close() propagates through ThreadedRepositoryBuilder → CachingBackend → SqliteBackend,
    // releasing the file lock so @TempDir cleanup succeeds on Windows.
  }

  @Test
  @SuppressWarnings("removal") // intentionally exercises the deprecated Module... factory
  void getLocalDataStoreRegistersProvidedModules(@TempDir Path tempDir) throws Exception {
    SimpleModule module = new SimpleModule().addSerializer(String.class, new UpperCaseSerializer());
    try (DataStore store = DataStore.getLocalDataStore("mods", tempDir.toFile(), module)) {
      Repository<String, String> repo =
          store.repository("ns", String.class, KeySerializers.forString());
      repo.put("k", "hello").get();
      // Without the module the value round-trips unchanged; "HELLO" proves it reached the mapper.
      assertEquals("HELLO", repo.get("k").get().orElseThrow());
    }
  }

  /** Test serializer that upper-cases strings, used to observe module registration end-to-end. */
  private static final class UpperCaseSerializer extends StdSerializer<String> {
    UpperCaseSerializer() {
      super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toUpperCase(Locale.ROOT));
    }
  }
}
