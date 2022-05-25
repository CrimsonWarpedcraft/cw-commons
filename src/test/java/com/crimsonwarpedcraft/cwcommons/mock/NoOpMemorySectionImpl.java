package com.crimsonwarpedcraft.cwcommons.mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationOptions;
import org.bukkit.configuration.MemorySection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Noop implementation of Configuration and MemorySection.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class NoOpMemorySectionImpl extends MemorySection implements Configuration {
  @Override
  public void addDefaults(@NotNull Map<String, Object> defaults) {

  }

  @Override
  public void addDefaults(@NotNull Configuration defaults) {

  }

  @Override
  public void setDefaults(@NotNull Configuration defaults) {

  }

  @Override
  public @Nullable Configuration getDefaults() {
    return null;
  }

  @SuppressFBWarnings(
      value = "NP_NONNULL_RETURN_VIOLATION",
      justification = "We never use this method"
  )
  @Override
  public @NotNull ConfigurationOptions options() {
    return null;
  }
}
