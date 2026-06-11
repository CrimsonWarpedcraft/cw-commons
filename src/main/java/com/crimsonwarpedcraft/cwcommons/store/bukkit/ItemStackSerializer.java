package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;

/**
 * Jackson serializer for Bukkit {@link ItemStack} values.
 *
 * <p>Annotate fields in your data classes to use it:
 * <pre>{@code
 * @JsonSerialize(using = ItemStackSerializer.class)
 * @JsonDeserialize(using = ItemStackDeserializer.class)
 * private ItemStack heldItem;
 * }</pre>
 *
 * <p>The item is encoded using {@link ItemStack#serializeAsBytes()} and stored as a Base64 string,
 * which preserves all NBT data.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class ItemStackSerializer extends StdSerializer<ItemStack> {

  /** Creates an {@link ItemStackSerializer}. */
  public ItemStackSerializer() {
    super(ItemStack.class);
  }

  @Override
  public void serialize(ItemStack item, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeString(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
  }
}
