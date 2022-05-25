package com.crimsonwarpedcraft.cwcommons.config;

/**
 * Consumes a value and validates it, throwing a ConfigurationException if it is invalid.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@FunctionalInterface
interface ValidityChecker<T> {
  void validate(T o) throws ConfigurationException;
}
