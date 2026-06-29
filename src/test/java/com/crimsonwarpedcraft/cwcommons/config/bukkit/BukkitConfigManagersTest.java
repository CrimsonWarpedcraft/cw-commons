package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.crimsonwarpedcraft.cwcommons.config.Config;
import com.crimsonwarpedcraft.cwcommons.config.ConfigManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class BukkitConfigManagersTest {

  record LocationConfig(Location spawn) implements Config {}

  record OrientedConfig(@RequireOrientation Location spawn) implements Config {}

  @Test
  void loadsLocationFromYamlWithLoadedWorld(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config.yml");
    Files.writeString(file, "spawn:\n  world: world\n  x: 0.5\n  y: 64.0\n  z: 0.5\n");
    World world = mock(World.class);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);

      LocationConfig cfg =
          BukkitConfigManagers.create().load(file.toFile(), LocationConfig.class);

      assertNotNull(cfg.spawn());
      assertEquals(world, cfg.spawn().getWorld());
      assertEquals(0.5, cfg.spawn().getX());
      assertEquals(64.0, cfg.spawn().getY());
      assertEquals(0.5, cfg.spawn().getZ());
      assertEquals(0.0f, cfg.spawn().getYaw(), 0.001f);   // yaw omitted -> default 0
      assertEquals(0.0f, cfg.spawn().getPitch(), 0.001f); // pitch omitted -> default 0
    }
  }

  @Test
  void rejectsLocationMissingOrientation(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config.yml");
    Files.writeString(file, "spawn:\n  world: world\n  x: 0.5\n  y: 64.0\n  z: 0.5\n");
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(mock(World.class));

      ConfigManager manager = BukkitConfigManagers.create();
      assertThrows(IllegalStateException.class,
          () -> manager.load(file.toFile(), OrientedConfig.class));
    }
  }

  @Test
  void loadsLocationWithRequiredOrientation(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config.yml");
    Files.writeString(file,
        "spawn:\n  world: world\n  x: 0.5\n  y: 64.0\n  z: 0.5\n  yaw: 90.0\n  pitch: -30.0\n");
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(mock(World.class));

      OrientedConfig cfg =
          BukkitConfigManagers.create().load(file.toFile(), OrientedConfig.class);

      assertEquals(90.0f, cfg.spawn().getYaw(), 0.001f);
      assertEquals(-30.0f, cfg.spawn().getPitch(), 0.001f);
    }
  }
}
