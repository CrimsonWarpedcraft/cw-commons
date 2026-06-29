package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Jackson {@link SimpleModule} bundling every (de)serializer this package provides for Bukkit
 * types: {@link Location} and {@link ItemStack}.
 *
 * <p>Register it once on an {@code ObjectMapper} to persist those types anywhere in your value
 * objects:
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper().registerModule(new BukkitModule());
 * }</pre>
 *
 * <p>Most plugins don't need to register it manually — {@code BukkitDataStores} (data store) and
 * {@code BukkitConfigManagers} (config) wire it in for you.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class BukkitModule extends SimpleModule {

  /** Creates a {@link BukkitModule} with all Bukkit (de)serializers registered. */
  public BukkitModule() {
    addSerializer(Location.class, new LocationSerializer());
    addDeserializer(Location.class, new LocationDeserializer());
    addSerializer(ItemStack.class, new ItemStackSerializer());
    addDeserializer(ItemStack.class, new ItemStackDeserializer());
  }
}
