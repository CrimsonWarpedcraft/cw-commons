package com.crimsonwarpedcraft.cwcommons.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class ThreadedRepository<K, V> implements Repository<K, V> {

  private final String namespace;
  private final Class<V> valueType;
  private final KeySerializer<K> keySerializer;
  private final StorageBackend backend;
  private final Executor ioExecutor;
  private final ObjectMapper mapper;

  ThreadedRepository(
      String namespace,
      Class<V> valueType,
      KeySerializer<K> keySerializer,
      StorageBackend backend,
      Executor ioExecutor,
      ObjectMapper mapper) {
    this.namespace = namespace;
    this.valueType = valueType;
    this.keySerializer = keySerializer;
    this.backend = backend;
    this.ioExecutor = ioExecutor;
    this.mapper = mapper;
  }

  @Override
  public CompletableFuture<Optional<V>> get(K key) {
    String rawKey = keySerializer.serialize(Objects.requireNonNull(key));
    return CompletableFuture.supplyAsync(() -> {
      try {
        Optional<String> json = backend.load(namespace, rawKey);
        if (json.isEmpty()) {
          return Optional.empty();
        }
        return Optional.of(mapper.readValue(json.get(), valueType));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, ioExecutor);
  }

  @Override
  public CompletableFuture<Map<K, V>> getAll() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Map<String, String> backendAll = backend.loadAll(namespace);
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<String, String> entry : backendAll.entrySet()) {
          K k = keySerializer.deserialize(entry.getKey());
          V v = mapper.readValue(entry.getValue(), valueType);
          result.put(k, v);
        }
        return result;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, ioExecutor);
  }

  @Override
  public CompletableFuture<Void> put(K key, V value) {
    String rawKey = keySerializer.serialize(Objects.requireNonNull(key));
    Objects.requireNonNull(value);
    return CompletableFuture.runAsync(() -> {
      try {
        backend.save(namespace, rawKey, mapper.writeValueAsString(value));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, ioExecutor);
  }

  @Override
  public CompletableFuture<Void> delete(K key) {
    String rawKey = keySerializer.serialize(Objects.requireNonNull(key));
    return CompletableFuture.runAsync(() -> {
      try {
        backend.delete(namespace, rawKey);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, ioExecutor);
  }

  @Override
  public CompletableFuture<Void> flush() {
    return CompletableFuture.runAsync(() -> {
      try {
        backend.flush(namespace);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, ioExecutor);
  }

}
