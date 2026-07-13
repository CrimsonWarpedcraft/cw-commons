# CwCommons

Shared library for CrimsonWarpedcraft Bukkit plugins. It bundles the infrastructure that every
plugin ends up rewriting — **configuration loading**, **command registration**, and **persistent
data storage** — behind small, well-tested APIs, so each plugin can focus on its own behaviour.

[![Test and Publish Snapshot](https://github.com/CrimsonWarpedcraft/cw-commons/actions/workflows/main.yml/badge.svg)](https://github.com/CrimsonWarpedcraft/cw-commons/actions/workflows/main.yml)
[![Release](https://jitpack.io/v/CrimsonWarpedcraft/cw-commons.svg)](https://jitpack.io/#CrimsonWarpedcraft/cw-commons)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://adoptium.net/)
[![Documentation](https://img.shields.io/badge/docs-cw--commons.crimsonwarpedcraft.com-blue)](https://cw-commons.crimsonwarpedcraft.com)
[![License: GPL v3](https://img.shields.io/github/license/CrimsonWarpedcraft/cw-commons)](LICENSE)


[![](https://dcbadge.limes.pink/api/server/5XMmeV6EtJ)](https://discord.gg/5XMmeV6EtJ)

📖 **Full documentation, guides, and API reference:**
[cw-commons.crimsonwarpedcraft.com](https://cw-commons.crimsonwarpedcraft.com)

## Features

The three areas below are independent — depend on one, two, or all of them. Each links to its
[guide](https://cw-commons.crimsonwarpedcraft.com) for a full walkthrough.

### Configuration ([guide](https://cw-commons.crimsonwarpedcraft.com/examples/config-loading.html))

- **`ConfigManager`** — reads a YAML file into a plain Java object and validates it with Jakarta
  Bean Validation in one call. Any POJO that implements `Config` and declares JSR-380 constraints
  (e.g. `@NotBlank`, `@Min`) works; a violation fails fast with a readable message instead of a
  half-initialised plugin. Also exposes `validate(...)` to check an object you built in code.
- **`@WorldExists`** — constraint for Bukkit `Location` fields: validation fails unless the
  location's world is loaded, so a config can't point at a missing world.
- **`@RequireOrientation`** — constraint for Bukkit `Location` fields: validation fails unless the
  config author supplied `yaw` and `pitch` (both default to `0` otherwise).
- **`BukkitConfigManagerBuilder`** — the standard entry point; wires up the YAML mapper, validator,
  and Bukkit `Location`/`ItemStack` binding.

### Commands ([guide](https://cw-commons.crimsonwarpedcraft.com/examples/commands.html))

- **`BaseCommand`** — thin base class that wraps a `CommandAPICommand` and implements the
  `Command` registration interface, so every command in every plugin is declared and registered the
  same way and `onEnable()` reads as a flat list of `register()` calls.

### Data storage ([guide](https://cw-commons.crimsonwarpedcraft.com/examples/store.html))

- **`DataStore`** — asynchronous, write-behind key-value store split into namespaced `Repository`
  views. Reads and writes return `CompletableFuture` and never touch disk on the main thread; writes
  are buffered in memory and flushed off-thread.
- **Pluggable backends** — `SqliteBackend` is bundled and zero-config; `MongoDbBackend` is a drop-in
  alternative behind the same API (needs the optional MongoDB driver).
- **`CacheMode`** — `CACHE_AND_FLUSH` (default) batches writes until a flush; `WRITE_THROUGH_ATOMIC`
  persists every write immediately for crash-safety; `NONE` disables caching entirely so a database
  shared across servers stays coherent (every read hits the backend fresh).
- **`KeySerializers`** — UUID and String keys out of the box; implement `KeySerializer<K>` for any
  other key type.
- **Bukkit helpers** — `PlayerDataManager` (flushes a player's data on `PlayerQuitEvent`),
  `AutoFlushTask` (periodic flush via the Bukkit scheduler), and `BukkitDataStoreBuilder`, which
  assembles a managed SQLite store with sensible defaults in one line.

### Bukkit types ([guide](https://cw-commons.crimsonwarpedcraft.com/examples/bukkit-types.html))

- **`BukkitModule`** — Jackson (de)serializers for `Location` and `ItemStack`, shared by both config
  loading and the data store so those types "just work" wherever they appear.

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
unreleased build from `main`. cw-commons targets **Java 25** and is built against the Paper API,
so it runs on any Paper-compatible server.

Jackson, Hibernate Validator, and Jakarta Validation are provided as transitive dependencies —
no need to declare them separately. If you shade your plugin, include and relocate them under
your own namespace to avoid conflicts with other plugins on the same server. CommandAPI is not
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
MyConfig config = new BukkitConfigManagerBuilder().build().load(configFile, MyConfig.class);
```

`ConfigManager` throws `IOException` if the file cannot be read and `IllegalStateException` if any
declared constraint is violated.

### Validating Bukkit locations

Constrain a Bukkit `Location` field with `@WorldExists` (the world must be loaded) and/or
`@RequireOrientation` (`yaw`/`pitch` must be supplied). A `null` location passes either check, so add
`@NotNull` to also require the value to be present.

```java
public class MyConfig implements Config {
    @WorldExists          // fails unless the world is loaded
    @RequireOrientation   // fails unless yaw and pitch are set
    private Location spawn;
}
```

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

### Data storage

```java
// In onEnable() — a SQLite-backed store with the Bukkit serializers pre-registered
DataStore store = new BukkitDataStoreBuilder("myplugin", getDataFolder()).build();

// A typed view over one namespace, keyed by player UUID
Repository<UUID, Integer> kills =
    store.repository("kills", Integer.class, KeySerializers.forUuid());

kills.put(player.getUniqueId(), 42);                    // buffered, flushed off-thread
kills.get(player.getUniqueId())                         // CompletableFuture<Optional<Integer>>
    .thenAccept(k -> getLogger().info("Kills: " + k.orElse(0)));

// In onDisable() — flushes pending writes, then releases the backend
store.close();
```

Futures complete on the store's I/O thread, so hop back to the main thread before touching the
Bukkit API. For per-player data, `PlayerDataManager` and `AutoFlushTask` handle flushing for you —
see the [Data Store guide](https://cw-commons.crimsonwarpedcraft.com/examples/store.html).

## Contributing

Contributions are welcome — whether you're fixing a typo, filing a bug, or adding a feature.
You don't need commit access: **fork the repo, make your change on a branch, and open a pull
request.** A good place to start is the
[issue tracker](https://github.com/CrimsonWarpedcraft/cw-commons/issues) (look for
[`good first issue`](https://github.com/CrimsonWarpedcraft/cw-commons/issues?q=is%3Aopen+label%3A%22good+first+issue%22)).

```bash
# Build, run static analysis (Checkstyle + SpotBugs), and run unit tests
./gradlew build        # macOS / Linux / Unix
gradlew.bat build      # Windows
```

The external-service integration suite is intentionally separate from `build`. It requires a
pre-provisioned MongoDB service and explicit connection environment variables; see
**[CONTRIBUTING.md](CONTRIBUTING.md#building--checks)** for setup and the `integrationTest` command.

See **[CONTRIBUTING.md](CONTRIBUTING.md)** for the full workflow — the fork-and-PR steps, the
Conventional Commit format we use, the local build and checks, and how to preview the docs.

## Community

Questions, ideas, or just want to say hi? Join us on
[Discord](https://discord.gg/5XMmeV6EtJ).

## License

CwCommons is licensed under the [GNU General Public License v3.0](LICENSE).
