package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.KeySerializers;
import com.crimsonwarpedcraft.cwcommons.store.Repository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class BukkitDataStoreBuilderIntegrationTest {

  @Test
  void persistsBukkitValuesAcrossManagedStoreReopen(@TempDir Path dataDir) throws Exception {
    World world = mock(World.class);
    when(world.getName()).thenReturn("world");
    UUID key = UUID.randomUUID();
    Location expected = new Location(world, 1.5, 64.0, -2.5, 90.0f, -30.0f);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
      try (DataStore store = openStore(dataDir)) {
        Repository<UUID, LocationValue> repository =
            store.repository("locations", LocationValue.class, KeySerializers.forUuid());
        repository.put(key, new LocationValue(expected)).get();
      }

      assertTrue(Files.isRegularFile(dataDir.resolve("plugin.db")));
      try (DataStore store = openStore(dataDir)) {
        Repository<UUID, LocationValue> repository =
            store.repository("locations", LocationValue.class, KeySerializers.forUuid());
        Location actual = repository.get(key).get().orElseThrow().location();
        assertEquals(world, actual.getWorld());
        assertEquals(expected.getX(), actual.getX());
        assertEquals(expected.getY(), actual.getY());
        assertEquals(expected.getZ(), actual.getZ());
        assertEquals(expected.getYaw(), actual.getYaw());
        assertEquals(expected.getPitch(), actual.getPitch());
      }
    }
  }

  private static DataStore openStore(Path dataDir) throws Exception {
    return new BukkitDataStoreBuilder("plugin", dataDir.toFile())
        .executor(Runnable::run)
        .build();
  }

  record LocationValue(Location location) {}
}
