package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
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
 * <p>Items are written in Bukkit's {@code ConfigurationSerializable} map form — a readable object
 * (e.g. {@code type: STICK, amount: 64}). Metadata (name, lore, enchantments) nests under
 * {@code meta} with Bukkit's {@code ==} type markers, matching how Bukkit writes items to YAML.
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
    Map<String, Object> plain = new LinkedHashMap<>();
    item.serialize().forEach((key, value) -> plain.put(key, toPlain(value)));
    gen.writeObject(plain);
  }

  /**
   * Recursively converts a Bukkit serialization graph to plain {@link Map}/{@link List}/scalar
   * values, tagging nested {@link ConfigurationSerializable} objects with the {@code ==} marker.
   */
  private static Object toPlain(Object value) {
    if (value instanceof ConfigurationSerializable serializable) {
      Map<String, Object> plain = new LinkedHashMap<>();
      plain.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY,
          ConfigurationSerialization.getAlias(serializable.getClass()));
      serializable.serialize().forEach((key, nested) -> plain.put(key, toPlain(nested)));
      return plain;
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> plain = new LinkedHashMap<>();
      map.forEach((key, nested) -> plain.put(String.valueOf(key), toPlain(nested)));
      return plain;
    }
    if (value instanceof List<?> list) {
      List<Object> plain = new ArrayList<>(list.size());
      list.forEach(element -> plain.add(toPlain(element)));
      return plain;
    }
    return value;
  }
}
