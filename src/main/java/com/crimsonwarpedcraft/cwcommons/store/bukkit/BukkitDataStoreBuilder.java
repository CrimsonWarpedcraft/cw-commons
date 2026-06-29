package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.crimsonwarpedcraft.cwcommons.bukkit.serialization.BukkitModule;
import com.crimsonwarpedcraft.cwcommons.store.DataStoreBuilder;
import com.crimsonwarpedcraft.cwcommons.store.SqliteBackend;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * A {@link DataStoreBuilder} pre-wired for Bukkit plugins — the recommended entry point for the
 * common case.
 *
 * <p>It backs the store with a local SQLite file ({@code <name>.db}), registers
 * {@link BukkitModule} so {@link org.bukkit.Location} and {@link org.bukkit.inventory.ItemStack}
 * values just work, and owns the backend so
 * {@link com.crimsonwarpedcraft.cwcommons.store.DataStore#close()} closes the database. Tweak the
 * write policy, mapper, or executor via the inherited setters before
 * {@code build()}:
 * <pre>{@code
 * DataStore store = new BukkitDataStoreBuilder(getName(), getDataFolder()).build();
 * }</pre>
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class BukkitDataStoreBuilder extends DataStoreBuilder {

  /**
   * Creates a builder backed by {@code <name>.db} in {@code dataDir}.
   *
   * @param name the plugin or store name; used as the database filename ({@code <name>.db})
   *             and the I/O thread name ({@code <name>-store-io})
   * @param dataDir the directory in which to create the database file
   * @throws IOException if the SQLite database cannot be created or opened
   */
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN",
      justification = "name and dataDir are caller-supplied; callers are responsible for "
          + "ensuring these values do not contain path traversal sequences")
  public BukkitDataStoreBuilder(String name, File dataDir) throws IOException {
    super(new SqliteBackend(
        new File(Objects.requireNonNull(dataDir), Objects.requireNonNull(name) + ".db")));
    name(name);
    mapper(defaultMapper().registerModule(new BukkitModule()));
  }

  private static ObjectMapper defaultMapper() {
    return new ObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
}
