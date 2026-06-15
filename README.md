# CwCommons

Shared library for CrimsonWarpedCraft Bukkit plugins. Provides reusable infrastructure for
configuration loading and command registration.

[![Test](https://github.com/CrimsonWarpedcraft/cw-commons/actions/workflows/pr.yml/badge.svg)](https://github.com/CrimsonWarpedcraft/cw-commons/actions/workflows/pr.yml)
[![Test and Publish Snapshot](https://github.com/CrimsonWarpedcraft/cw-commons/actions/workflows/main.yml/badge.svg)](https://github.com/CrimsonWarpedcraft/cw-commons/actions/workflows/main.yml)

## Features

- **`ConfigManager`** — loads and validates a YAML config file using Jackson and Jakarta Bean
  Validation. Any POJO that implements `Config` and declares JSR-380 constraints (e.g. `@NotBlank`)
  can be used.
- **`BaseCommand`** — thin base class that wraps a `CommandAPICommand` and implements the
  `Command` registration interface, so every plugin command follows the same pattern.
- **`DataStore`** — write-behind key-value store with namespaced `Repository` instances. SQLite is
  bundled; MongoDB is supported via an optional driver. Bukkit helpers include
  `PlayerDataManager` (flushes on `PlayerQuitEvent`) and `AutoFlushTask` (periodic flush via the
  Bukkit scheduler). Custom Jackson serializers for `Location` and `ItemStack` are included.

## Adding as a dependency

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.CrimsonWarpedcraft:cw-commons:VERSION")
}
```

Replace `VERSION` with a release tag (e.g. `v1.0.0`) or `main-SNAPSHOT` for the latest
unreleased build from `main`.

The library JAR already shades and relocates Jackson and Hibernate Validator. CommandAPI is not
included — shade it in your own plugin as you normally would, and it will be compatible with
`BaseCommand`.

## Usage

### Config loading

```java
// Define your config POJO
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyConfig implements Config {
    @NotBlank
    @JsonProperty("welcome-message")
    private String welcomeMessage = "Welcome!";

    public String getWelcomeMessage() { return welcomeMessage; }
}

// Load and validate in onEnable()
MyConfig config = new ConfigManager().load(configFile, MyConfig.class);
```

`ConfigManager` throws `IOException` if the file cannot be read and `IllegalStateException` if any
declared constraint is violated.

### Command registration

```java
public class MyCommand extends BaseCommand {
    public MyCommand() {
        super(
            new CommandAPICommand("mycommand")
                .withPermission("myplugin.use")
                .executes((sender, args) -> sender.sendRichMessage("Hello!"))
        );
    }
}

// In onEnable()
new MyCommand().register();
```

## Contributing

### General workflow

1. Pull any changes from `main` to make sure you're up to date.
2. Create a branch from `main` with a descriptive name (e.g. `add-scoreboard`). One change per
   branch; keep commits small with clear messages.
3. Open a pull request to `main` with a descriptive title and a summary of what changed. Link any
   related issues.

After review, approval, and passing all automated checks the PR will be merged.

### Building locally

```
# macOS / Linux / Unix
./gradlew build

# Windows
gradlew.bat build
```

The build step also runs all static-analysis checks (Checkstyle, SpotBugs) and unit tests.
