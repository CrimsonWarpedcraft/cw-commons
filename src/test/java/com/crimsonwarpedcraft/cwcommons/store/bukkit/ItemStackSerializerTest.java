package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemStackSerializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(ItemStack.class, new ItemStackSerializer());
    mapper = new ObjectMapper().registerModule(module);
  }

  @Test
  void serializesAsBase64String() throws IOException {
    byte[] bytes = {1, 2, 3, 4};
    ItemStack mockItem = mock(ItemStack.class);
    when(mockItem.serializeAsBytes()).thenReturn(bytes);

    String json = mapper.writeValueAsString(mockItem);

    assertEquals("\"" + Base64.getEncoder().encodeToString(bytes) + "\"", json);
  }

  @Test
  void differentBytesProduceDifferentOutput() throws IOException {
    byte[] bytes1 = {1, 2, 3};
    byte[] bytes2 = {4, 5, 6};
    ItemStack item1 = mock(ItemStack.class);
    ItemStack item2 = mock(ItemStack.class);
    when(item1.serializeAsBytes()).thenReturn(bytes1);
    when(item2.serializeAsBytes()).thenReturn(bytes2);

    String json1 = mapper.writeValueAsString(item1);
    String json2 = mapper.writeValueAsString(item2);

    assertEquals("\"" + Base64.getEncoder().encodeToString(bytes1) + "\"", json1);
    assertEquals("\"" + Base64.getEncoder().encodeToString(bytes2) + "\"", json2);
  }
}
