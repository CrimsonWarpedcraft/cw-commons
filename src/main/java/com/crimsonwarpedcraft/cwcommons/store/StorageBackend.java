package com.crimsonwarpedcraft.cwcommons.store;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Low-level storage provider used by the data store to persist serialized JSON documents.
 *
 * <p>Each document is identified by a {@code namespace} (collection/table name) and a string
 * {@code key}. Values are opaque JSON strings; serialization is handled by the layer above.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public interface StorageBackend extends AutoCloseable {

  /**
   * Persists a JSON document, creating or replacing any existing document with the same key.
   *
   * @param namespace the logical collection name
   * @param key the document key
   * @param json the JSON string to store
   * @throws IOException if the document cannot be written
   */
  void save(String namespace, String key, String json) throws IOException;

  /**
   * Loads the JSON document with the given key, if present.
   *
   * @param namespace the logical collection name
   * @param key the document key
   * @return the stored JSON string, or empty if no document exists for this key
   * @throws IOException if the document cannot be read
   */
  Optional<String> load(String namespace, String key) throws IOException;

  /**
   * Loads all documents in the given namespace.
   *
   * @param namespace the logical collection name
   * @return a map of key to JSON string for every document in the namespace
   * @throws IOException if the documents cannot be read
   */
  Map<String, String> loadAll(String namespace) throws IOException;

  /**
   * Deletes the document with the given key, if it exists.
   *
   * @param namespace the logical collection name
   * @param key the document key
   * @throws IOException if the document cannot be deleted
   */
  void delete(String namespace, String key) throws IOException;

  /**
   * Flushes any buffered writes for the given namespace to the underlying storage.
   *
   * <p>The default implementation is a no-op; backends that buffer writes (e.g.
   * {@link CachingBackend}) override this method.
   *
   * @param namespace the logical collection name
   * @throws IOException if buffered writes cannot be persisted
   */
  default void flush(String namespace) throws IOException {}

  /**
   * Closes this backend and releases any underlying resources.
   *
   * @throws IOException if the backend cannot be closed cleanly
   */
  @Override
  void close() throws IOException;
}
