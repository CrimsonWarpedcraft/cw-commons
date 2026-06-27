package com.crimsonwarpedcraft.cwcommons.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A key-value store organized into namespaced {@link Repository} instances.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public interface DataStore extends AutoCloseable {

  /**
   * Returns a {@link Repository} for the given namespace.
   *
   * <p>Calling this method multiple times with the same namespace returns the same instance.
   * Do not reuse the same namespace with different value types.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param namespace a unique name for this collection (e.g. {@code "players"})
   * @param type the class of the value type, used for JSON deserialization
   * @param keySerializer converts keys to and from their string storage representation
   * @return the repository for this namespace
   */
  <K, V> Repository<K, V> repository(
      String namespace, Class<V> type, KeySerializer<K> keySerializer);

  /**
   * Flushes all pending writes in all repositories to the backend.
   *
   * @return a future completing when all dirty entries have been persisted
   */
  CompletableFuture<Void> flush();

  /**
   * Flushes all pending writes and releases any resources owned by the store.
   *
   * <p>Must be called from {@code onDisable()} to avoid data loss.
   *
   * @throws Exception if flush or resource release fails
   */
  @Override
  void close() throws Exception;

  /**
   * Creates a {@link DataStore} backed by a local SQLite file with sensible defaults.
   *
   * <p>The returned store owns the underlying {@link SqliteBackend}; calling {@link #close()}
   * flushes all dirty data and closes the database connection.
   *
   * @param name the plugin or store name; used as the database filename ({@code <name>.db})
   *             and the I/O thread name ({@code <name>-store-io})
   * @param dataDir the directory in which to create the database file
   * @return a ready-to-use store
   * @throws IOException if the SQLite database cannot be created or opened
   * @deprecated For Bukkit plugins (the common case) use {@link
   *     com.crimsonwarpedcraft.cwcommons.store.bukkit.BukkitDataStores}, which also registers the
   *     Bukkit (de)serializers. For non-Bukkit or custom setups, assemble the store yourself from
   *     {@link ConcurrentDataStore} and {@link ThreadedRepositoryBuilder}.
   */
  @Deprecated(forRemoval = true)
  static DataStore getLocalDataStore(String name, File dataDir) throws IOException {
    return getLocalDataStore(name, dataDir, new Module[0]);
  }

  /**
   * Creates a {@link DataStore} backed by a local SQLite file, registering the given Jackson
   * modules on its mapper.
   *
   * <p>The returned store owns the backend; {@link #close()} flushes and closes it.
   *
   * @param name the plugin or store name; used as the database filename ({@code <name>.db})
   *             and the I/O thread name ({@code <name>-store-io})
   * @param dataDir the directory in which to create the database file
   * @param modules Jackson modules to register on the store's mapper
   * @return a ready-to-use store
   * @throws IOException if the SQLite database cannot be created or opened
   * @deprecated For Bukkit plugins (the common case) use {@link
   *     com.crimsonwarpedcraft.cwcommons.store.bukkit.BukkitDataStores}. For custom serializers or
   *     backends, assemble the store yourself from {@link ConcurrentDataStore} and
   *     {@link ThreadedRepositoryBuilder}.
   */
  @Deprecated(forRemoval = true)
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN",
      justification = "name and dataDir are caller-supplied; callers are responsible for "
          + "ensuring these values do not contain path traversal sequences")
  static DataStore getLocalDataStore(String name, File dataDir, Module... modules)
      throws IOException {
    SqliteBackend sb = new SqliteBackend(
        new File(Objects.requireNonNull(dataDir), Objects.requireNonNull(name) + ".db")
    );
    CachingBackend cb = new CachingBackend(sb, WritePolicy.CACHE_AND_FLUSH);
    ObjectMapper mapper = defaultMapper();
    for (Module module : modules) {
      mapper.registerModule(Objects.requireNonNull(module));
    }
    ThreadedRepositoryBuilder trb =
        new ThreadedRepositoryBuilder(cb, buildExecutor(name), mapper, true);
    return new ConcurrentDataStore(trb);
  }

  private static ExecutorService buildExecutor(String name) {
    return Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, name + "-store-io");
      t.setDaemon(true);
      return t;
    });
  }

  private static ObjectMapper defaultMapper() {
    return new ObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
}
