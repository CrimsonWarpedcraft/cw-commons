package com.crimsonwarpedcraft.cwcommons.mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mock object for WorldBorders.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
public class MockWorldBorder implements WorldBorder {
  private volatile double size = 0;
  private long resizeDuration = 0;

  @Override
  public @Nullable World getWorld() {
    return null;
  }

  @Override
  public void reset() {

  }

  @Override
  public double getSize() {
    return size;
  }

  @Override
  public void setSize(double newSize) {
    size = newSize;
  }

  @Override
  public void setSize(double newSize, long seconds) {
    size = newSize;
    resizeDuration = seconds;
  }

  @Override
  public @NotNull Location getCenter() {
    return null;
  }

  @Override
  public void setCenter(double x, double z) {

  }

  @Override
  public void setCenter(@NotNull Location location) {

  }

  @Override
  public double getDamageBuffer() {
    return 0;
  }

  @Override
  public void setDamageBuffer(double blocks) {

  }

  @Override
  public double getDamageAmount() {
    return 0;
  }

  @Override
  public void setDamageAmount(double damage) {

  }

  @Override
  public int getWarningTime() {
    return 0;
  }

  @Override
  public void setWarningTime(int seconds) {

  }

  @Override
  public int getWarningDistance() {
    return 0;
  }

  @Override
  public void setWarningDistance(int distance) {

  }

  @Override
  public boolean isInside(@NotNull Location location) {
    return false;
  }

  public long getResizeDuration() {
    return resizeDuration;
  }
}
