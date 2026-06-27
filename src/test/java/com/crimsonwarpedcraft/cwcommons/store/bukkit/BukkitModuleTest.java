package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class BukkitModuleTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper().registerModule(new BukkitModule());
  }

  @Test
  void registersLocationSerializer() throws IOException {
    World world = mock(World.class);
    when(world.getName()).thenReturn("overworld");
    Location loc = mock(Location.class);
    when(loc.getWorld()).thenReturn(world);
    when(loc.getX()).thenReturn(1.0);
    when(loc.getY()).thenReturn(64.0);
    when(loc.getZ()).thenReturn(-3.0);
    when(loc.getYaw()).thenReturn(90.0f);
    when(loc.getPitch()).thenReturn(-30.0f);

    JsonNode node = mapper.readTree(mapper.writeValueAsString(loc));

    assertEquals("overworld", node.get("world").asText());
    assertEquals(1.0, node.get("x").asDouble());
  }

  @Test
  void registersLocationDeserializer() throws IOException {
    String json = "{\"world\":\"overworld\",\"x\":1.0,\"y\":64.0,\"z\":-3.0,"
        + "\"yaw\":90.0,\"pitch\":-30.0}";
    World world = mock(World.class);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(() -> Bukkit.getWorld("overworld")).thenReturn(world);

      Location loc = mapper.readValue(json, Location.class);

      assertSame(world, loc.getWorld());
      assertEquals(1.0, loc.getX());
    }
  }

  @Test
  void registersItemStackSerializer() throws IOException {
    byte[] bytes = {1, 2, 3, 4};
    ItemStack item = mock(ItemStack.class);
    when(item.serializeAsBytes()).thenReturn(bytes);

    String json = mapper.writeValueAsString(item);

    assertEquals("\"" + Base64.getEncoder().encodeToString(bytes) + "\"", json);
  }

  @Test
  void registersItemStackDeserializer() throws IOException {
    byte[] bytes = {10, 20, 30};
    String json = "\"" + Base64.getEncoder().encodeToString(bytes) + "\"";
    ItemStack item = mock(ItemStack.class);

    try (MockedStatic<ItemStack> itemStackStatic = mockStatic(ItemStack.class)) {
      itemStackStatic
          .when(() -> ItemStack.deserializeBytes(any(byte[].class)))
          .thenReturn(item);

      ItemStack result = mapper.readValue(json, ItemStack.class);

      assertSame(item, result);
    }
  }
}
