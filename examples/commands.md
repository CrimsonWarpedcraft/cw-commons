# Commands

`BaseCommand` wraps a pre-built `CommandAPICommand` and implements the `Command` registration
interface. CommandAPI must be shaded into your own plugin — it is **not** included in the
cw-commons JAR.

## Define a command

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

```java
new GreetCommand().register();
```

## Build dependency

CommandAPI is `compileOnly` in cw-commons. Add it (shaded) to your own plugin:

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.jorel:commandapi-bukkit-shade:11.2.0")
}
```
