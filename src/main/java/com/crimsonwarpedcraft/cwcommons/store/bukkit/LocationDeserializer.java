package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Jackson deserializer for Bukkit {@link Location} values.
 *
 * <p>Reads the format produced by {@link LocationSerializer}. If the stored world name does not
 * match a loaded world at deserialization time the {@code world} field of the returned
 * {@link Location} will be {@code null}.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class LocationDeserializer extends StdDeserializer<Location> {

  /** Creates a {@link LocationDeserializer}. */
  public LocationDeserializer() {
    super(Location.class);
  }

  @Override
  public Location deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);
    JsonNode worldNode = node.get("world");
    World world = null;
    if (worldNode != null && !worldNode.isNull()) {
      world = Bukkit.getWorld(worldNode.asText());
    }
    double x = node.get("x").asDouble();
    double y = node.get("y").asDouble();
    double z = node.get("z").asDouble();
    float yaw = (float) node.get("yaw").asDouble();
    float pitch = (float) node.get("pitch").asDouble();
    return new Location(world, x, y, z, yaw, pitch);
  }
}
