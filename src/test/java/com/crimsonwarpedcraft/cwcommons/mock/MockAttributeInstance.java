package com.crimsonwarpedcraft.cwcommons.mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Mock object for max health AttributeInstance.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
public class MockAttributeInstance implements AttributeInstance {
  private final Attribute attribute;
  private final double defaultValue;
  private double value;

  /** Create a new MockAttributeInstance of the provided attribute. */
  public MockAttributeInstance(Attribute attribute, double defaultValue, double value) {
    this.attribute = attribute;
    this.defaultValue = defaultValue;
    this.value = value;
  }

  @Override
  public @NotNull Attribute getAttribute() {
    return attribute;
  }

  @Override
  public double getBaseValue() {
    return value;
  }

  @Override
  public void setBaseValue(double value) {
    this.value = value;
  }

  @Override
  public @NotNull Collection<AttributeModifier> getModifiers() {
    return null;
  }

  @Override
  public void addModifier(@NotNull AttributeModifier modifier) {

  }

  @Override
  public void removeModifier(@NotNull AttributeModifier modifier) {

  }

  @Override
  public double getValue() {
    return value;
  }

  @Override
  public double getDefaultValue() {
    return defaultValue;
  }
}
