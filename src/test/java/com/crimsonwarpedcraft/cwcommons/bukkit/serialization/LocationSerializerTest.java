package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocationSerializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(Location.class, new LocationSerializer());
    mapper = new ObjectMapper().registerModule(module);
  }

  private Location mockLocation(World world, double x, double y, double z,
      float yaw, float pitch) {
    Location loc = mock(Location.class);
    when(loc.getWorld()).thenReturn(world);
    when(loc.getX()).thenReturn(x);
    when(loc.getY()).thenReturn(y);
    when(loc.getZ()).thenReturn(z);
    when(loc.getYaw()).thenReturn(yaw);
    when(loc.getPitch()).thenReturn(pitch);
    return loc;
  }

  @Test
  void serializesAllFields() throws IOException {
    World world = mock(World.class);
    when(world.getName()).thenReturn("overworld");
    Location loc = mockLocation(world, 1.0, 64.0, -3.0, 90.0f, -30.0f);

    JsonNode node = mapper.readTree(mapper.writeValueAsString(loc));

    assertEquals("overworld", node.get("world").asText());
    assertEquals(1.0, node.get("x").asDouble());
    assertEquals(64.0, node.get("y").asDouble());
    assertEquals(-3.0, node.get("z").asDouble());
    assertEquals(90.0, node.get("yaw").asDouble(), 0.001);
    assertEquals(-30.0, node.get("pitch").asDouble(), 0.001);
  }

  @Test
  void serializesNullWorldAsNull() throws IOException {
    Location loc = mockLocation(null, 0.0, 0.0, 0.0, 0.0f, 0.0f);

    JsonNode node = mapper.readTree(mapper.writeValueAsString(loc));

    assertTrue(node.get("world").isNull());
  }
}
