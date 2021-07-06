package com.crimsonwarpedcraft.cwcommons.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import java.util.Objects;
import org.bukkit.plugin.Plugin;

/**
 * Used to register PaperMC game commands.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class PaperCommandRegister {
  private final PaperCommandManager manager;

  /**
   * Returns a new instance of a PaperCommandRegister.
   *
   * @param plugin the plugin that is registering the command
   * @return a new CommandRegister instance
   */
  public static PaperCommandRegister getNewPaperCommandRegister(Plugin plugin) {
    return new PaperCommandRegister(Objects.requireNonNull(plugin));
  }

  private PaperCommandRegister(Plugin plugin) {
    manager = new PaperCommandManager(plugin);
  }

  /** Registers a command. */
  public PaperCommandRegister register(BaseCommand command) {
    manager.registerCommand(Objects.requireNonNull(command));

    return this;
  }
}
