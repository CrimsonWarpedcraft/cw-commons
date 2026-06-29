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
 * Requires the annotated Bukkit {@link Location}'s {@code yaw} and {@code pitch} to be present in
 * the configuration.
 *
 * <p>Place it on a {@link Location} field of a
 * {@link com.crimsonwarpedcraft.cwcommons.config.Config} so
 * {@link com.crimsonwarpedcraft.cwcommons.config.ConfigManager} rejects a configuration that omits
 * the location's orientation:
 * <pre>{@code
 * @RequireOrientation
 * @JsonProperty("spawn")
 * private Location spawn;
 * }</pre>
 *
 * <p>Without this annotation an absent {@code yaw}/{@code pitch} defaults to {@code 0}, which keeps
 * hand-written config terse. When present, the bundled
 * {@link com.crimsonwarpedcraft.cwcommons.bukkit.serialization.LocationDeserializer} marks an
 * absent {@code yaw} or {@code pitch} with {@link Float#NaN} so validation can detect the
 * omission, and the check fails. A {@code null} location is valid — combine it with
 * {@link jakarta.validation.constraints.NotNull} to require presence. The annotation only takes
 * effect on a mapper using that deserializer, such as one from
 * {@code BukkitConfigManagerBuilder}.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = RequireOrientationValidator.class)
public @interface RequireOrientation {

  /**
   * Returns the message rendered when yaw or pitch is missing.
   *
   * @return the constraint violation message template
   */
  String message() default "must specify yaw and pitch";

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
