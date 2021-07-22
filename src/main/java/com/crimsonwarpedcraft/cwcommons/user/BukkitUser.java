package com.crimsonwarpedcraft.cwcommons.user;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Represents a user of the plugin.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class BukkitUser {
  private final CommandSender sender;

  protected BukkitUser(CommandSender sender) {
    this.sender = Objects.requireNonNull(sender);
  }

  /** Sends the user a message. */
  public BukkitUser sendMessage(Component message) {
    sender.sendMessage(Objects.requireNonNull(message));

    return this;
  }

  /** Returns the user's name. */
  public String getName() {
    return sender.getName();
  }

  protected CommandSender getSender() {
    return sender;
  }
}
