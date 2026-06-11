package com.crimsonwarpedcraft.cwcommons.command;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.jorel.commandapi.CommandAPICommand;
import org.junit.jupiter.api.Test;

class BaseCommandTest {

  @Test
  void nullCommandThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new BaseCommand(null));
  }

  @Test
  void registerCallsCommandRegister() {
    CommandAPICommand mockCommand = mock(CommandAPICommand.class);
    new BaseCommand(mockCommand).register();
    verify(mockCommand).register();
  }

  @Test
  void registerReturnsSelf() {
    CommandAPICommand mockCommand = mock(CommandAPICommand.class);
    BaseCommand baseCommand = new BaseCommand(mockCommand);
    assertSame(baseCommand, baseCommand.register());
  }
}
