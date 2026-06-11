package com.crimsonwarpedcraft.cwcommons.store;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bson.Document;

/**
 * A {@link StorageBackend} backed by a MongoDB database.
 *
 * <p>Requires {@code org.mongodb:mongodb-driver-sync} on the classpath at runtime.
 *
 * <p>Usage:
 * <pre>{@code
 * MongoDbBackend backend =
 *     new MongoDbBackend("mongodb://localhost:27017", "myDatabase");
 * DataStore store = new ConcurrentDataStore(
 *     new ThreadedRepositoryBuilder(
 *         new CachingBackend(backend, WritePolicy.CACHE_AND_FLUSH),
 *         Executors.newSingleThreadExecutor(),
 *         new ObjectMapper()));
 * }</pre>
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class MongoDbBackend implements StorageBackend {

  private final MongoClient client;
  private final MongoDatabase database;

  /**
   * Creates a {@link MongoDbBackend} connected to the given MongoDB database.
   *
   * @param uri the MongoDB connection URI (e.g. {@code "mongodb://localhost:27017"})
   * @param databaseName the name of the database to use
   * @throws IOException if the connection cannot be established
   */
  public MongoDbBackend(String uri, String databaseName) throws IOException {
    Objects.requireNonNull(uri);
    Objects.requireNonNull(databaseName);
    try {
      client = MongoClients.create(uri);
      database = client.getDatabase(databaseName);
    } catch (Exception e) {
      throw new IOException("Failed to connect to MongoDB at " + uri, e);
    }
  }

  @Override
  public void save(String namespace, String key, String json) throws IOException {
    try {
      MongoCollection<Document> col = database.getCollection(namespace);
      Document doc = new Document("_id", key).append("value", json);
      col.replaceOne(Filters.eq("_id", key), doc, new ReplaceOptions().upsert(true));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Optional<String> load(String namespace, String key) throws IOException {
    try {
      MongoCollection<Document> col = database.getCollection(namespace);
      Document doc = col.find(Filters.eq("_id", key)).first();
      if (doc == null) {
        return Optional.empty();
      }
      return Optional.of(doc.getString("value"));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public Map<String, String> loadAll(String namespace) throws IOException {
    try {
      MongoCollection<Document> col = database.getCollection(namespace);
      Map<String, String> result = new HashMap<>();
      for (Document doc : col.find()) {
        result.put(doc.getString("_id"), doc.getString("value"));
      }
      return result;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void delete(String namespace, String key) throws IOException {
    try {
      database.getCollection(namespace).deleteOne(Filters.eq("_id", key));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      client.close();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
}
