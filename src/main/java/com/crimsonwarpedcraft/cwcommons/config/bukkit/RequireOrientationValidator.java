package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.bukkit.Location;

/**
 * Validates a {@link RequireOrientation} constraint by requiring the location's {@code yaw} and
 * {@code pitch} to be present.
 *
 * <p>The bundled
 * {@link com.crimsonwarpedcraft.cwcommons.bukkit.serialization.LocationDeserializer} represents an
 * absent {@code yaw} or {@code pitch} as {@link Float#NaN} for fields carrying this constraint,
 * so a {@code NaN} means the value was omitted. A {@code null} location is treated as valid,
 * following the Jakarta Bean Validation convention that {@code null} elements are left to
 * {@link jakarta.validation.constraints.NotNull}.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class RequireOrientationValidator
    implements ConstraintValidator<RequireOrientation, Location> {

  @Override
  public boolean isValid(Location value, ConstraintValidatorContext context) {
    return value == null || (!Float.isNaN(value.getYaw()) && !Float.isNaN(value.getPitch()));
  }
}
