package com.crimsonwarpedcraft.cwcommons.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.annotations.Command;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Used to register Bukkit game commands.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class CommandRegister {
  private final List<Class<? extends BaseCommand>> commands;

  /**
   * Returns a new instance of a CommandRegister.
   *
   * @return a new CommandRegister instance
   */
  public static CommandRegister getNewCommandRegister() {
    return new CommandRegister();
  }

  protected CommandRegister() {
    this(List.of());
  }

  protected CommandRegister(List<Class<? extends BaseCommand>> commands) {
    this.commands = List.copyOf(commands);
  }

  /**
   * Register a command.
   *
   * @param command the class of the command to register
   * @return a new instance of this class with the registered command
   * @throws CommandException if the command has already been registered
   */
  public CommandRegister register(Class<? extends BaseCommand> command) {
    Objects.requireNonNull(command);

    if (commands.contains(command)) {
      throw new CommandException("Class " + command.getName() + " has already been registered!");
    }

    CommandAPI.registerCommand(command);

    List<Class<? extends BaseCommand>> commands = new ArrayList<>(this.commands);
    commands.add(command);
    return new CommandRegister(commands);
  }

  /** Unregisters all registered commands. */
  public CommandRegister unregisterAll() {
    CommandRegister register = new CommandRegister(commands);

    while (!register.commands.isEmpty()) {
      register = unregister(register.commands.get(0));
    }

    return register;
  }

  /**
   * Unregisters a registered command.
   *
   * @param command the class of the command to unregister
   * @return a new instance of this class with the command unregistered
   * @throws CommandException if the command has not been registered
   */
  public CommandRegister unregister(Class<? extends BaseCommand> command) {
    Objects.requireNonNull(command);

    if (!commands.contains(command)) {
      throw new CommandException("Class " + command.getName() + " has not been registered!");
    }

    Command commandAnnotation = Objects.requireNonNull(command.getAnnotation(Command.class));
    String value = Objects.requireNonNull(commandAnnotation.value());

    CommandAPI.unregister(value);

    List<Class<? extends BaseCommand>> commands = new ArrayList<>(this.commands);
    commands.remove(command);
    return new CommandRegister(commands);
  }
}
