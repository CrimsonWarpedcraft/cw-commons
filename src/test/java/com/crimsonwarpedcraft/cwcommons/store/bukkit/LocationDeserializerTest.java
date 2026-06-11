package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class LocationDeserializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Location.class, new LocationDeserializer());
    mapper = new ObjectMapper().registerModule(module);
  }

  private static final String BASE_JSON =
      "{\"world\":\"%s\",\"x\":1.0,\"y\":64.0,\"z\":-3.0,\"yaw\":90.0,\"pitch\":-30.0}";

  @Test
  void deserializesAllFieldsWithKnownWorld() throws IOException {
    World mockWorld = mock(World.class);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getWorld("overworld")).thenReturn(mockWorld);

      Location loc = mapper.readValue(String.format(BASE_JSON, "overworld"), Location.class);

      assertEquals(mockWorld, loc.getWorld());
      assertEquals(1.0, loc.getX());
      assertEquals(64.0, loc.getY());
      assertEquals(-3.0, loc.getZ());
      assertEquals(90.0f, loc.getYaw(), 0.001f);
      assertEquals(-30.0f, loc.getPitch(), 0.001f);
    }
  }

  @Test
  void unknownWorldNameYieldsNullWorld() throws IOException {
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getWorld("unknown")).thenReturn(null);

      Location loc = mapper.readValue(String.format(BASE_JSON, "unknown"), Location.class);

      assertNull(loc.getWorld());
    }
  }

  @Test
  void nullWorldNodeYieldsNullWorld() throws IOException {
    String json = "{\"world\":null,\"x\":0.0,\"y\":0.0,\"z\":0.0,\"yaw\":0.0,\"pitch\":0.0}";

    Location loc = mapper.readValue(json, Location.class);

    assertNull(loc.getWorld());
  }

  @Test
  void missingWorldNodeYieldsNullWorld() throws IOException {
    String json = "{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"yaw\":0.0,\"pitch\":0.0}";

    Location loc = mapper.readValue(json, Location.class);

    assertNull(loc.getWorld());
  }
}
