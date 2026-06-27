---
title: Home
nav_order: 1
---

# CwCommons

Shared library for CrimsonWarpedCraft Bukkit plugins. Provides reusable infrastructure for
configuration loading, command registration, and persistent data storage.

[API Reference](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/){: .btn .btn-primary }
[Get started](getting-started.md){: .btn }

---

## Features

- **`ConfigManager`** — loads and validates a YAML config file using Jackson and Jakarta Bean
  Validation. Any POJO that implements `Config` and declares JSR-380 constraints (e.g. `@NotBlank`)
  can be used. See [Config Loading](examples/config-loading.md).
- **`BaseCommand`** — thin base class that wraps a `CommandAPICommand` and implements the
  `Command` registration interface, so every plugin command follows the same pattern. See
  [Commands](examples/commands.md).
- **`DataStore`** — write-behind key-value store with namespaced `Repository` instances. SQLite is
  bundled; MongoDB is supported via an optional driver. Bukkit helpers include `PlayerDataManager`
  (flushes on `PlayerQuitEvent`) and `AutoFlushTask` (periodic flush via the Bukkit scheduler).
  Custom Jackson serializers for `Location` and `ItemStack` are included. See the
  [storage examples](examples/store-sqlite.md).

## Documentation

- **[Getting Started](getting-started.md)** — add cw-commons as a dependency.
- **[Examples](examples/index.md)** — task-focused guides for config, commands, and storage.
- **[API Reference](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/)** —
  full Javadoc for the latest release, hosted by JitPack.
  [Older versions](https://jitpack.io/#CrimsonWarpedcraft/cw-commons) are kept too.
