---
title: Commands
parent: Examples
nav_order: 2
---

# Commands

`BaseCommand` is a thin adapter between [CommandAPI](https://commandapi.jorel.dev/) and a uniform
registration interface. You build a `CommandAPICommand` as usual, hand it to `BaseCommand`, and get
a `Command` whose `register()` method plugs into the same lifecycle as every other command in your
plugin. The point is consistency: every command is defined and registered the same way, so
`onEnable()` reads as a flat list of `register()` calls.

CommandAPI must be shaded into your own plugin — it is **not** included in the cw-commons JAR
(see [Build dependency](#build-dependency) below).

## Define a command

Extend `BaseCommand` and pass a fully-configured `CommandAPICommand` to `super(...)`:

```java
import com.crimsonwarpedcraft.cwcommons.command.BaseCommand;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;

public class GreetCommand extends BaseCommand {

    public GreetCommand() {
        super(
            new CommandAPICommand("greet")
                .withPermission("myplugin.greet")
                .withArguments(new StringArgument("name"))
                .executes((sender, args) -> {
                    String name = (String) args.get("name");
                    sender.sendRichMessage("<green>Hello, " + name + "!</green>");
                })
        );
    }
}
```

## Register in `onEnable()`

`register()` returns the command instance, so calls chain cleanly:

```java
new GreetCommand().register();
```

> **Timing matters.** CommandAPI must be loaded before you register anything. Call
> `CommandAPI.onLoad(...)` from your plugin's `onLoad()` and `CommandAPI.onEnable()` at the top of
> `onEnable()`, then run your `register()` calls. See the
> [CommandAPI setup docs](https://commandapi.jorel.dev/) for the exact bootstrap.

## Why an interface?

`BaseCommand` implements `Command`, which declares a single `register()` method. Coding against
`Command` keeps your registration site uniform and lets you collect commands in a list:

```java
List<Command> commands = List.of(new GreetCommand(), new OtherCommand());
commands.forEach(Command::register);
```

`BaseCommand` is intentionally non-`final` so you can subclass it; the wrapped `CommandAPICommand`
is owned by your subclass.

## Build dependency

CommandAPI is `compileOnly` in cw-commons. Add it (shaded) to your own plugin — pick the shaded
artifact that matches your server platform:

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.jorel:commandapi-bukkit-shade:11.2.0") // or commandapi-paper-shade
}
```

See [Getting Started](../getting-started.md#optional-dependencies) for the full list of optional
dependencies.
