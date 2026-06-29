---
title: Config Loading
parent: Examples
nav_order: 1
---

# Config Loading

`ConfigManager` does three things in one call: it reads a YAML file, deserializes it into a plain
Java object with Jackson, and validates that object against its JSR-380 constraint annotations.
If anything is wrong you find out immediately, in `onEnable()`, instead of hitting a bad value
deep in your plugin later.

- `load(...)` throws **`IOException`** if the file is missing, unreadable, or not valid YAML.
- `load(...)` and `validate(...)` throw **`IllegalStateException`** if any constraint is violated.
  The message lists every offending property, e.g.
  `Invalid configuration: maxPlayers: must be less than or equal to 100`.

## Define a config class

A config class is any POJO that implements the marker interface
[`Config`](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/). Add
Jackson annotations to map YAML keys and Jakarta Bean Validation annotations to declare what counts
as valid. Field defaults act as fallbacks for keys omitted from the file.

```java
import com.crimsonwarpedcraft.cwcommons.config.Config;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginConfig implements Config {

    @NotBlank
    @JsonProperty("server-name")
    private String serverName = "My Server";

    @Min(1) @Max(100)
    @JsonProperty("max-players")
    private int maxPlayers = 20;

    public String getServerName() { return serverName; }
    public int getMaxPlayers() { return maxPlayers; }
}
```

A few things worth knowing about how `ConfigManager` reads this class:

- **No getters/setters required for binding.** The manager's `ObjectMapper` is configured to read
  and write *fields* directly (`Visibility.ANY`), so private fields are populated even without
  accessors. The getters above are for *your* code to call.
- **`@JsonProperty` maps kebab-case keys.** `server-name` in YAML binds to the `serverName` field.
  Without the annotation Jackson would look for a `servername`/`serverName` key.
- **`@JsonIgnoreProperties(ignoreUnknown = true)` is recommended.** Unlike the data store's mapper,
  the config mapper does **not** silently ignore unknown keys — without this annotation a stray or
  misspelled key in the file throws an `IOException`. Keep it unless you *want* typos to fail loudly.

## Load in `onEnable()`

```java
import com.crimsonwarpedcraft.cwcommons.config.bukkit.BukkitConfigManagerBuilder;
import java.io.File;

// Write the bundled default config.yml if the file doesn't exist yet
saveDefaultConfig();
File configFile = new File(getDataFolder(), "config.yml");

PluginConfig config = new BukkitConfigManagerBuilder().build().load(configFile, PluginConfig.class);
getLogger().info("Server name: " + config.getServerName());
```

`saveDefaultConfig()` is the standard Bukkit `JavaPlugin` helper that copies the `config.yml` from
your JAR's resources into the data folder on first run. `ConfigManager` itself only reads files; it
never writes defaults.

`BukkitConfigManagerBuilder` is the standard way to build a `ConfigManager` — it wires up the YAML
mapper, a validator, and Bukkit `Location`/`ItemStack` support (see
[Loading Bukkit types](#loading-bukkit-types)).

A robust `onEnable()` turns a bad config into a clean shutdown rather than a stack trace:

```java
PluginConfig config;
try {
    config = new BukkitConfigManagerBuilder().build().load(configFile, PluginConfig.class);
} catch (IOException | IllegalStateException e) {
    getLogger().severe("Failed to load config: " + e.getMessage());
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

## `config.yml`

```yaml
server-name: "Survival World"
max-players: 50
```

## Validate an object without loading from disk

`validate()` runs the same constraint check on an object you built yourself — handy for config you
assemble in code, or for unit tests.

```java
PluginConfig cfg = new PluginConfig();
new BukkitConfigManagerBuilder().build().validate(cfg); // throws IllegalStateException if invalid
```

## Loading Bukkit types

Config classes can bind Bukkit `Location` and `ItemStack` fields too — `BukkitConfigManagerBuilder`
registers the serializers for you. See [Bukkit Types](bukkit-types.md) for the field formats and a
full example.

## Requiring a loaded world

`@WorldExists` is a constraint for Bukkit `Location` fields: validation fails unless the location's
world is loaded (its `Location#getWorld()` is non-null — which is how the bundled
[`LocationDeserializer`](store-bukkit.md#storing-bukkit-types) represents an unloaded world). A
`null` location passes, so pair it with `@NotNull` to also require presence.

```java
import com.crimsonwarpedcraft.cwcommons.config.Config;
import com.crimsonwarpedcraft.cwcommons.config.bukkit.WorldExists;
import org.bukkit.Location;

public class SpawnConfig implements Config {
    @WorldExists
    private Location spawn;
}
```

Load with `new BukkitConfigManagerBuilder().build()` — it binds the `Location` from YAML via `BukkitModule` and
runs the constraint during `load()`, so loading throws `IllegalStateException` if the configured
world isn't loaded:

```java
SpawnConfig config = new BukkitConfigManagerBuilder().build().load(configFile, SpawnConfig.class);
// throws IllegalStateException if spawn's world isn't loaded
```

## Requiring orientation

By default a `Location`'s `yaw` and `pitch` are optional in YAML and default to `0` when omitted,
which keeps config terse. For a field where orientation matters, `@RequireOrientation` makes the
config author spell them out — validation fails if either is missing:

```java
import com.crimsonwarpedcraft.cwcommons.config.Config;
import com.crimsonwarpedcraft.cwcommons.config.bukkit.RequireOrientation;
import org.bukkit.Location;

public class SpawnConfig implements Config {
    @RequireOrientation
    private Location spawn;
}
```

`new BukkitConfigManagerBuilder().build()` binds the `Location` via the bundled `LocationDeserializer`, which
marks an omitted `yaw`/`pitch` so the constraint can detect it during `load()`:

```java
SpawnConfig config = new BukkitConfigManagerBuilder().build().load(configFile, SpawnConfig.class);
// throws IllegalStateException if spawn omits yaw or pitch
```

A `null` location passes, so pair it with `@NotNull` to also require presence.

## Dependencies

Jackson, Hibernate Validator, and Jakarta Validation arrive transitively — see
[Getting Started](../getting-started.md#transitive-dependencies). If you shade your plugin, relocate
them under your own namespace to avoid conflicts with other plugins on the same server.
