package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.bukkit.Location;

/**
 * Validates a {@link WorldExists} constraint by requiring the location's world to be loaded.
 *
 * <p>A {@code null} location is treated as valid, following the Jakarta Bean Validation convention
 * that {@code null} elements are left to {@link jakarta.validation.constraints.NotNull}.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class WorldExistsValidator implements ConstraintValidator<WorldExists, Location> {

  @Override
  public boolean isValid(Location value, ConstraintValidatorContext context) {
    return value == null || value.getWorld() != null;
  }
}
