package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ItemStackDeserializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(ItemStack.class, new ItemStackDeserializer());
    mapper = new ObjectMapper().registerModule(module);
  }

  @Test
  void deserializesSimpleItem() throws IOException {
    String json = "{\"type\":\"STICK\",\"amount\":64}";
    ItemStack expected = mock(ItemStack.class);

    try (MockedStatic<ConfigurationSerialization> cs =
        mockStatic(ConfigurationSerialization.class)) {
      cs.when(() -> ConfigurationSerialization.deserializeObject(anyMap(), eq(ItemStack.class)))
          .thenReturn(expected);

      ItemStack result = mapper.readValue(json, ItemStack.class);

      assertSame(expected, result);
    }
  }

  @Test
  void reconstructsNestedMetaBeforeBuilding() throws IOException {
    String json = "{\"type\":\"DIAMOND_SWORD\",\"meta\":"
        + "{\"==\":\"ItemMeta\",\"display-name\":\"Excalibur\"}}";
    ItemMeta meta = mock(ItemMeta.class);
    ItemStack expected = mock(ItemStack.class);

    try (MockedStatic<ConfigurationSerialization> cs =
        mockStatic(ConfigurationSerialization.class)) {
      cs.when(() -> ConfigurationSerialization.deserializeObject(anyMap())).thenReturn(meta);
      cs.when(() -> ConfigurationSerialization.deserializeObject(anyMap(), eq(ItemStack.class)))
          .thenReturn(expected);

      ItemStack result = mapper.readValue(json, ItemStack.class);

      assertSame(expected, result);
      cs.verify(() -> ConfigurationSerialization.deserializeObject(anyMap()));
    }
  }
}
