package com.crimsonwarpedcraft.cwcommons.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class ThreadedRepository<K, V> implements Repository<K, V> {

  private final String namespace;
  private final Class<V> valueType;
  private final KeySerializer<K> keySerializer;
  private final StorageBackend backend;
  private final StoreScheduler scheduler;
  private final ObjectMapper mapper;

  ThreadedRepository(
      String namespace,
      Class<V> valueType,
      KeySerializer<K> keySerializer,
      StorageBackend backend,
      StoreScheduler scheduler,
      ObjectMapper mapper) {
    this.namespace = namespace;
    this.valueType = valueType;
    this.keySerializer = keySerializer;
    this.backend = backend;
    this.scheduler = scheduler;
    this.mapper = mapper;
  }

  @Override
  public CompletableFuture<Optional<V>> get(K key) {
    String rawKey = keySerializer.serialize(Objects.requireNonNull(key));
    return scheduler.submit(() -> {
      Optional<String> json = backend.load(namespace, rawKey);
      if (json.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(mapper.readValue(json.get(), valueType));
    });
  }

  @Override
  public CompletableFuture<Map<K, V>> getAll() {
    return scheduler.submit(() -> {
      Map<String, String> backendAll = backend.loadAll(namespace);
      Map<K, V> result = new HashMap<>();
      for (Map.Entry<String, String> entry : backendAll.entrySet()) {
        K k = keySerializer.deserialize(entry.getKey());
        V v = mapper.readValue(entry.getValue(), valueType);
        result.put(k, v);
      }
      return result;
    });
  }

  @Override
  public CompletableFuture<Void> put(K key, V value) {
    String rawKey = keySerializer.serialize(Objects.requireNonNull(key));
    Objects.requireNonNull(value);
    return scheduler.submit(() -> {
      backend.save(namespace, rawKey, mapper.writeValueAsString(value));
      return null;
    });
  }

  @Override
  public CompletableFuture<Void> delete(K key) {
    String rawKey = keySerializer.serialize(Objects.requireNonNull(key));
    return scheduler.submit(() -> {
      backend.delete(namespace, rawKey);
      return null;
    });
  }

  @Override
  public CompletableFuture<Void> flush() {
    return scheduler.submit(() -> {
      backend.flush(namespace);
      return null;
    });
  }

}
