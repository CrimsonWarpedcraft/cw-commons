package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import com.crimsonwarpedcraft.cwcommons.config.bukkit.RequireOrientation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
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
 * <p>{@code world}, {@code x}, {@code y}, and {@code z} are required; {@code yaw} and {@code pitch}
 * are optional and default to {@code 0} when absent, which keeps hand-written config terse.
 *
 * <p>This deserializer is contextual: when the target field is annotated with
 * {@link RequireOrientation}, an absent {@code yaw} or {@code pitch} is read as {@link Float#NaN}
 * instead of {@code 0} so that the {@code @RequireOrientation} constraint can detect the omission.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class LocationDeserializer extends StdDeserializer<Location>
    implements ContextualDeserializer {

  private final float missingOrientation;

  /**
   * Creates a {@link LocationDeserializer} that defaults an absent {@code yaw} or {@code pitch}
   * to {@code 0}.
   */
  public LocationDeserializer() {
    this(0.0f);
  }

  /**
   * Creates a {@link LocationDeserializer} that substitutes the given value for an absent or null
   * {@code yaw} or {@code pitch}.
   *
   * @param missingOrientation the value used when {@code yaw} or {@code pitch} is absent; pass
   *     {@link Float#NaN} to mark orientation as missing for {@code @RequireOrientation} validation
   */
  public LocationDeserializer(float missingOrientation) {
    super(Location.class);
    this.missingOrientation = missingOrientation;
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
    if (property != null && property.getAnnotation(RequireOrientation.class) != null) {
      return new LocationDeserializer(Float.NaN);
    }
    return this;
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
    float yaw = readOptionalFloat(node.get("yaw"));
    float pitch = readOptionalFloat(node.get("pitch"));
    return new Location(world, x, y, z, yaw, pitch);
  }

  private float readOptionalFloat(JsonNode node) {
    return node != null && !node.isNull() ? (float) node.asDouble() : missingOrientation;
  }
}
