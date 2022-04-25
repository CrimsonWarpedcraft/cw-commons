package com.crimsonwarpedcraft.cwcommons.command;

import java.util.Objects;

/**
 * Exception thrown when there is an issue registering or unregistering commands.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class CommandException extends RuntimeException {
  public CommandException(String message) {
    super(Objects.requireNonNull(message));
  }
}
