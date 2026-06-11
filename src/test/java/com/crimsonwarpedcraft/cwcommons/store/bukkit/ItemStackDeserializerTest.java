package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;
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
  void deserializeCallsDeserializeBytesAndPropagatesResult() throws IOException {
    byte[] bytes = {10, 20, 30};
    String json = "\"" + Base64.getEncoder().encodeToString(bytes) + "\"";
    ItemStack mockItem = mock(ItemStack.class);

    try (MockedStatic<ItemStack> itemStackStatic = mockStatic(ItemStack.class)) {
      itemStackStatic
          .when(() -> ItemStack.deserializeBytes(any(byte[].class)))
          .thenReturn(mockItem);

      ItemStack result = mapper.readValue(json, ItemStack.class);

      assertSame(mockItem, result);
    }
  }
}
