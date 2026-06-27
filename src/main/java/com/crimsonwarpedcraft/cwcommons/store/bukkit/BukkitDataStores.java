package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.crimsonwarpedcraft.cwcommons.store.CachingBackend;
import com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStore;
import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.SqliteBackend;
import com.crimsonwarpedcraft.cwcommons.store.ThreadedRepositoryBuilder;
import com.crimsonwarpedcraft.cwcommons.store.WritePolicy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Factory for {@link DataStore} instances pre-wired for Bukkit plugins — the recommended entry
 * point for the common case.
 *
 * <p>It builds a managed, SQLite-backed, lifecycle-owning store and registers {@link BukkitModule}
 * on the mapper, so {@link org.bukkit.Location} and {@link org.bukkit.inventory.ItemStack} values
 * just work without any extra configuration. For a custom backend, write policy, executor, or set
 * of serializers, assemble the store yourself from {@link ConcurrentDataStore} and
 * {@link ThreadedRepositoryBuilder}.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class BukkitDataStores {

  private BukkitDataStores() {
  }

  /**
   * Creates a {@link DataStore} backed by a local SQLite file with the Bukkit (de)serializers
   * already registered.
   *
   * <p>The returned store owns the underlying backend; calling {@link DataStore#close()} flushes
   * all dirty data and closes the database connection.
   *
   * @param name the plugin or store name; used as the database filename ({@code <name>.db})
   *             and the I/O thread name ({@code <name>-store-io})
   * @param dataDir the directory in which to create the database file
   * @return a ready-to-use store that can persist {@code Location} and {@code ItemStack} values
   * @throws IOException if the SQLite database cannot be created or opened
   */
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN",
      justification = "name and dataDir are caller-supplied; callers are responsible for "
          + "ensuring these values do not contain path traversal sequences")
  public static DataStore getLocalDataStore(String name, File dataDir) throws IOException {
    SqliteBackend sb = new SqliteBackend(
        new File(Objects.requireNonNull(dataDir), Objects.requireNonNull(name) + ".db")
    );
    CachingBackend cb = new CachingBackend(sb, WritePolicy.CACHE_AND_FLUSH);
    ObjectMapper mapper = defaultMapper().registerModule(new BukkitModule());
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
