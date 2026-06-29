package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;

/**
 * Jackson deserializer for Bukkit {@link ItemStack} values.
 *
 * <p>Reads the {@code ConfigurationSerializable} map form produced by {@link ItemStackSerializer},
 * rebuilding nested metadata (the {@code ==}-tagged {@code meta} object) before constructing the
 * item, so display names, lore, and enchantments survive the round trip. A top-level {@code ==} is
 * optional, so hand-written configs can use just {@code type}/{@code amount}/{@code meta}.
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
    Map<?, ?> raw = parser.readValueAs(Map.class);
    Map<String, Object> args = new LinkedHashMap<>();
    raw.forEach((key, value) -> args.put(String.valueOf(key), reconstruct(value)));
    return (ItemStack) ConfigurationSerialization.deserializeObject(args, ItemStack.class);
  }

  /**
   * Recursively rebuilds a plain {@link Map}/{@link List} graph, turning any {@code ==}-tagged
   * sub-map back into the Bukkit object it represents (bottom-up), so the parent receives real
   * objects rather than raw maps.
   */
  private static Object reconstruct(Object value) {
    if (value instanceof Map<?, ?> rawMap) {
      Map<String, Object> map = new LinkedHashMap<>();
      rawMap.forEach((key, nested) -> map.put(String.valueOf(key), reconstruct(nested)));
      if (map.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
        return ConfigurationSerialization.deserializeObject(map);
      }
      return map;
    }
    if (value instanceof List<?> list) {
      List<Object> reconstructed = new ArrayList<>(list.size());
      list.forEach(element -> reconstructed.add(reconstruct(element)));
      return reconstructed;
    }
    return value;
  }
}
