# Config Loading

`ConfigManager` reads a YAML file, deserializes it into any POJO that implements `Config`, and
validates it using JSR-380 constraint annotations. An `IOException` is thrown if the file cannot be
read; `IllegalStateException` if any constraint is violated.

## Define a config class

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

## Load in `onEnable()`

```java
import com.crimsonwarpedcraft.cwcommons.config.ConfigManager;
import java.io.File;

// Write defaults if the file doesn't exist yet
saveDefaultConfig();
File configFile = new File(getDataFolder(), "config.yml");

PluginConfig config = new ConfigManager().load(configFile, PluginConfig.class);
getLogger().info("Server name: " + config.getServerName());
```

## `config.yml`

```yaml
server-name: "Survival World"
max-players: 50
```

## Validate an object without loading from disk

```java
PluginConfig cfg = new PluginConfig();
new ConfigManager().validate(cfg); // throws IllegalStateException if invalid
```

Jackson and Hibernate Validator are shaded into the library JAR and relocated, so they will not
conflict with other plugins.
