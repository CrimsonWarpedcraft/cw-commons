package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;

/**
 * Jackson deserializer for Bukkit {@link ItemStack} values.
 *
 * <p>Reads the Base64-encoded format produced by {@link ItemStackSerializer}.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class ItemStackDeserializer extends StdDeserializer<ItemStack> {

  /** Creates an {@link ItemStackDeserializer}. */
  public ItemStackDeserializer() {
    super(ItemStack.class);
  }

  @Override
  public ItemStack deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    byte[] bytes = Base64.getDecoder().decode(parser.getText());
    return ItemStack.deserializeBytes(bytes);
  }
}
