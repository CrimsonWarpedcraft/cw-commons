package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class AbstractDataStoreIntegrationTest {

  private String namespace;
  private Set<String> namespaces;

  @BeforeEach
  void createNamespaces() {
    namespace = "cw_commons_it_" + UUID.randomUUID().toString().replace("-", "");
    namespaces = new HashSet<>();
    namespaces.add(namespace);
  }

  @AfterEach
  void cleanUpNamespaces() throws IOException {
    for (String createdNamespace : namespaces) {
      cleanUp(createdNamespace);
    }
  }

  protected abstract StorageBackend openBackend() throws IOException;

  protected void cleanUp(String createdNamespace) throws IOException {}

  protected final String namespace() {
    return namespace;
  }

  protected final String namespace(String suffix) {
    String createdNamespace = namespace + "_" + suffix;
    namespaces.add(createdNamespace);
    return createdNamespace;
  }

  @Test
  void typedRepositoriesPersistAcrossBackendReopen() throws Exception {
    UUID firstKey = UUID.randomUUID();
    UUID secondKey = UUID.randomUUID();
    String otherNamespace = namespace("other");
    try (DataStore store = openStore(CacheMode.CACHE_AND_FLUSH)) {
      Repository<UUID, StoredValue> repository =
          store.repository(namespace(), StoredValue.class, KeySerializers.forUuid());
      Repository<UUID, StoredValue> other =
          store.repository(otherNamespace, StoredValue.class, KeySerializers.forUuid());
      repository.put(firstKey, new StoredValue("first", 1)).get();
      repository.put(secondKey, new StoredValue("second", 2)).get();
      other.put(firstKey, new StoredValue("other", 99)).get();
    }

    try (DataStore store = openStore(CacheMode.NONE)) {
      Repository<UUID, StoredValue> repository =
          store.repository(namespace(), StoredValue.class, KeySerializers.forUuid());
      Map<UUID, StoredValue> values = repository.getAll().get();
      assertEquals(2, values.size());
      assertEquals(new StoredValue("first", 1), values.get(firstKey));
      assertEquals(new StoredValue("second", 2), values.get(secondKey));
      repository.put(firstKey, new StoredValue("updated", 3)).get();
      repository.delete(secondKey).get();
    }

    try (DataStore store = openStore(CacheMode.NONE)) {
      Repository<UUID, StoredValue> repository =
          store.repository(namespace(), StoredValue.class, KeySerializers.forUuid());
      Repository<UUID, StoredValue> other =
          store.repository(otherNamespace, StoredValue.class, KeySerializers.forUuid());
      assertEquals(new StoredValue("updated", 3), repository.get(firstKey).get().orElseThrow());
      assertFalse(repository.get(secondKey).get().isPresent());
      assertEquals(new StoredValue("other", 99), other.get(firstKey).get().orElseThrow());
    }
  }

  @Test
  void cacheAndFlushBuffersWritesUntilFlush() throws Exception {
    try (StorageBackend observer = openBackend();
        DataStore store = openStore(CacheMode.CACHE_AND_FLUSH)) {
      Repository<String, String> repository =
          store.repository(namespace(), String.class, KeySerializers.forString());
      repository.put("key", "buffered").get();

      assertTrue(observer.load(namespace(), "key").isEmpty());
      store.flush().get();
      assertEquals("\"buffered\"", observer.load(namespace(), "key").orElseThrow());
    }
  }

  @Test
  void writeThroughPersistsWritesImmediately() throws Exception {
    try (StorageBackend observer = openBackend();
        DataStore store = openStore(CacheMode.WRITE_THROUGH_ATOMIC)) {
      Repository<String, String> repository =
          store.repository(namespace(), String.class, KeySerializers.forString());
      repository.put("key", "immediate").get();

      assertEquals("\"immediate\"", observer.load(namespace(), "key").orElseThrow());
    }
  }

  @Test
  void cacheModeNoneReadsFreshBackendValues() throws Exception {
    try (StorageBackend writer = openBackend();
        DataStore store = openStore(CacheMode.NONE)) {
      Repository<String, String> repository =
          store.repository(namespace(), String.class, KeySerializers.forString());
      writer.save(namespace(), "key", "\"first\"");
      assertEquals("first", repository.get("key").get().orElseThrow());

      writer.save(namespace(), "key", "\"second\"");
      assertEquals("second", repository.get("key").get().orElseThrow());
    }
  }

  private DataStore openStore(CacheMode cacheMode) throws IOException {
    return DataStore.builder(openBackend())
        .executor(Runnable::run)
        .cacheMode(cacheMode)
        .build();
  }

  record StoredValue(String name, int count) {}
}
