package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ItemStackSerializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(ItemStack.class, new ItemStackSerializer());
    mapper = new ObjectMapper().registerModule(module);
  }

  @Test
  void serializesSimpleItemAsMap() throws IOException {
    Map<String, Object> serialized = new LinkedHashMap<>();
    serialized.put("type", "STICK");
    serialized.put("amount", 64);
    ItemStack item = mock(ItemStack.class);
    when(item.serialize()).thenReturn(serialized);

    JsonNode node = mapper.readTree(mapper.writeValueAsString(item));

    assertEquals("STICK", node.get("type").asText());
    assertEquals(64, node.get("amount").asInt());
  }

  @Test
  void serializesNestedMetaWithTypeMarker() throws IOException {
    Map<String, Object> metaMap = new LinkedHashMap<>();
    metaMap.put("display-name", "Excalibur");
    ItemMeta meta = mock(ItemMeta.class);
    when(meta.serialize()).thenReturn(metaMap);

    Map<String, Object> serialized = new LinkedHashMap<>();
    serialized.put("type", "DIAMOND_SWORD");
    serialized.put("meta", meta);
    ItemStack item = mock(ItemStack.class);
    when(item.serialize()).thenReturn(serialized);

    try (MockedStatic<ConfigurationSerialization> cs =
        mockStatic(ConfigurationSerialization.class)) {
      cs.when(() -> ConfigurationSerialization.getAlias(any())).thenReturn("ItemMeta");

      JsonNode node = mapper.readTree(mapper.writeValueAsString(item));

      assertEquals("DIAMOND_SWORD", node.get("type").asText());
      JsonNode metaNode = node.get("meta");
      assertEquals("ItemMeta", metaNode.get("==").asText());
      assertEquals("Excalibur", metaNode.get("display-name").asText());
    }
  }
}
