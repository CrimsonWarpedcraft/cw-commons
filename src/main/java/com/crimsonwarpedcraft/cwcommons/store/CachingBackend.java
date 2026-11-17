package com.crimsonwarpedcraft.cwcommons.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link StorageBackend} decorator that caches reads and buffers writes in memory.
 *
 * <p>The {@link WritePolicy} applies to every namespace in this decorator.
 * {@link WritePolicy#CACHE_AND_FLUSH} buffers writes until {@link #flush(String)} is called;
 * {@link WritePolicy#WRITE_THROUGH_ATOMIC} calls the delegate on every
 * {@link #save(String, String, String)} and updates the cache only after that call succeeds.
 * Deletes likewise remove cached entries only after the delegate deletion succeeds.
 *
 * <p>A flush saves dirty entries and then flushes the delegate, even if nothing is cached locally.
 * Entries become clean only after all saves and the downstream flush succeed. On failure, dirty
 * entries remain available for retry, which may repeat already successful saves as overwrites.
 * This is not a multi-document transaction or rollback guarantee: a delegate may change storage
 * before throwing, so a failure does not establish whether a write committed. Durability and
 * atomicity depend on the delegate, especially if it buffers writes itself.
 *
 * <p>These guarantees assume callers serialize operations on this decorator and its delegate,
 * such as through a single store I/O executor. The concurrent maps do not make multi-step
 * operations atomic or coordinate independent callers sharing a backend.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class CachingBackend implements StorageBackend {

  private final StorageBackend delegate;
  private final ConcurrentHashMap<String, ConcurrentHashMap<String, CacheEntry<String>>> cache =
      new ConcurrentHashMap<>();
  private final WritePolicy policy;

  /**
   * Creates a {@link CachingBackend} wrapping the given delegate.
   *
   * @param delegate the backend to delegate reads and flushed writes to
   * @param policy when buffered writes are persisted to the delegate
   */
  CachingBackend(StorageBackend delegate, WritePolicy policy) {
    this.delegate = Objects.requireNonNull(delegate);
    this.policy = Objects.requireNonNull(policy);
  }

  @Override
  public void save(String namespace, String key, String json) throws IOException {
    boolean dirty = policy == WritePolicy.CACHE_AND_FLUSH;

    if (policy == WritePolicy.WRITE_THROUGH_ATOMIC) {
      delegate.save(namespace, key, json);
    }

    cache.computeIfAbsent(namespace, _ -> new ConcurrentHashMap<>())
        .put(key, new CacheEntry<>(json, dirty));
  }

  @Override
  public Optional<String> load(String namespace, String key) throws IOException {
    ConcurrentHashMap<String, CacheEntry<String>> nsCache =
        cache.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
    CacheEntry<String> entry = nsCache.get(key);
    if (entry != null) {
      return Optional.of(entry.value);
    }
    Optional<String> loaded = delegate.load(namespace, key);
    loaded.ifPresent(json -> nsCache.put(key, new CacheEntry<>(json, false)));
    return loaded;
  }

  @Override
  public Map<String, String> loadAll(String namespace) throws IOException {
    Map<String, String> backendAll = delegate.loadAll(namespace);
    ConcurrentHashMap<String, CacheEntry<String>> nsCache =
        cache.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> entry : backendAll.entrySet()) {
      CacheEntry<String> cached = nsCache.computeIfAbsent(
          entry.getKey(), k -> new CacheEntry<>(entry.getValue(), false));
      result.put(entry.getKey(), cached.value);
    }
    for (Map.Entry<String, CacheEntry<String>> entry : nsCache.entrySet()) {
      result.putIfAbsent(entry.getKey(), entry.getValue().value);
    }
    return result;
  }

  @Override
  public void delete(String namespace, String key) throws IOException {
    delegate.delete(namespace, key);
    ConcurrentHashMap<String, CacheEntry<String>> nsCache = cache.get(namespace);
    if (nsCache != null) {
      nsCache.remove(key);
    }
  }

  @Override
  public void flush(String namespace) throws IOException {
    ConcurrentHashMap<String, CacheEntry<String>> nsCache = cache.get(namespace);
    List<CacheEntry<String>> flushedEntries = new ArrayList<>();
    if (nsCache != null) {
      for (Map.Entry<String, CacheEntry<String>> entry : nsCache.entrySet()) {
        CacheEntry<String> ce = entry.getValue();
        if (ce.dirty) {
          delegate.save(namespace, entry.getKey(), ce.value);
          flushedEntries.add(ce);
        }
      }
    }
    delegate.flush(namespace);
    for (CacheEntry<String> entry : flushedEntries) {
      entry.dirty = false;
    }
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
