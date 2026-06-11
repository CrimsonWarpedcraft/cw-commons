package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.bukkit.Location;

/**
 * Jackson serializer for Bukkit {@link Location} values.
 *
 * <p>Annotate fields in your data classes to use it:
 * <pre>{@code
 * @JsonSerialize(using = LocationSerializer.class)
 * @JsonDeserialize(using = LocationDeserializer.class)
 * private Location homeLocation;
 * }</pre>
 *
 * <p>The JSON representation is:
 * <pre>{@code
 * {"world":"world","x":0.0,"y":64.0,"z":0.0,"yaw":0.0,"pitch":0.0}
 * }</pre>
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class LocationSerializer extends StdSerializer<Location> {

  /** Creates a {@link LocationSerializer}. */
  public LocationSerializer() {
    super(Location.class);
  }

  @Override
  public void serialize(Location location, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("world",
        location.getWorld() != null ? location.getWorld().getName() : null);
    gen.writeNumberField("x", location.getX());
    gen.writeNumberField("y", location.getY());
    gen.writeNumberField("z", location.getZ());
    gen.writeNumberField("yaw", location.getYaw());
    gen.writeNumberField("pitch", location.getPitch());
    gen.writeEndObject();
  }
}
