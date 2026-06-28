package com.crimsonwarpedcraft.cwcommons.config.bukkit;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.bukkit.Location;

/**
 * Requires the annotated Bukkit {@link Location}'s world to be loaded.
 *
 * <p>Place it on a {@link Location} field of a
 * {@link com.crimsonwarpedcraft.cwcommons.config.Config} so
 * {@link com.crimsonwarpedcraft.cwcommons.config.ConfigManager} rejects a configuration whose
 * location refers to a world the server has not loaded:
 * <pre>{@code
 * @WorldExists
 * @JsonProperty("spawn")
 * private Location spawn;
 * }</pre>
 *
 * <p>A {@code null} location is valid — combine it with
 * {@link jakarta.validation.constraints.NotNull} to require presence. Validation fails when the
 * location's {@link Location#getWorld() world} is {@code null}, which is how the bundled
 * location deserializer represents an unloaded world. Omit the annotation to skip the check.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = WorldExistsValidator.class)
public @interface WorldExists {

  /**
   * Returns the message rendered when the world is not loaded.
   *
   * @return the constraint violation message template
   */
  String message() default "must reference a loaded world";

  /**
   * Returns the validation groups this constraint belongs to.
   *
   * @return the validation groups
   */
  Class<?>[] groups() default {};

  /**
   * Returns the metadata payloads associated with this constraint.
   *
   * @return the constraint payloads
   */
  Class<? extends Payload>[] payload() default {};
}
