package com.crimsonwarpedcraft.cwcommons.bukkit.serialization;

import com.crimsonwarpedcraft.cwcommons.config.bukkit.RequireOrientation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.math.BigDecimal;
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
 * <p>{@code world} is optional. Coordinates must be finite numbers; quoted decimal numbers are
 * also accepted. Optional {@code yaw} and {@code pitch} default to {@code 0} when absent or null.
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
    if (!node.isObject()) {
      return ctxt.reportInputMismatch(Location.class, "Expected a location object with x, y and z");
    }
    double x = readNumber(node, "x", ctxt);
    double y = readNumber(node, "y", ctxt);
    double z = readNumber(node, "z", ctxt);
    float yaw = readOrientation(node, "yaw", ctxt);
    float pitch = readOrientation(node, "pitch", ctxt);
    JsonNode worldNode = node.get("world");
    World world = null;
    if (worldNode != null && !worldNode.isNull()) {
      if (!worldNode.isTextual()) {
        return ctxt.reportPropertyInputMismatch(Location.class, "world", "Expected a world name");
      }
      world = Bukkit.getWorld(worldNode.textValue());
    }
    return new Location(world, x, y, z, yaw, pitch);
  }

  private float readOrientation(JsonNode object, String field, DeserializationContext ctxt)
      throws JsonMappingException {
    if (!object.hasNonNull(field)) {
      return missingOrientation;
    }
    float value = (float) readNumber(object, field, ctxt);
    if (!Float.isFinite(value)) {
      return ctxt.reportPropertyInputMismatch(Location.class, field,
          "%s is outside the range of a finite float", field);
    }
    return value;
  }

  private static double readNumber(JsonNode object, String field, DeserializationContext ctxt)
      throws JsonMappingException {
    JsonNode node = object.path(field);
    if (node.isNumber() && Double.isFinite(node.doubleValue())) {
      return node.doubleValue();
    }
    if (node.isTextual()) {
      try {
        double value = new BigDecimal(node.textValue().trim()).doubleValue();
        if (Double.isFinite(value)) {
          return value;
        }
      } catch (NumberFormatException ignored) {
        // Report the same field-aware mapping error as for other non-numeric input.
      }
    }
    return ctxt.reportPropertyInputMismatch(Location.class, field,
        "Expected a finite number for %s", field);
  }
}
