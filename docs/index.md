---
title: Home
nav_order: 1
---

# CwCommons

Shared library for CrimsonWarpedcraft Bukkit plugins. It bundles the infrastructure that every
plugin ends up rewriting — **configuration loading**, **command registration**, and **persistent
data storage** — behind small, well-tested APIs so each plugin can focus on its own behaviour.

[API Reference](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/){: .btn .btn-primary }
[Get started](getting-started.md){: .btn }

---

## Features

- **`ConfigManager`** — loads a YAML file into a plain Java object and validates it with Jakarta
  Bean Validation. Any POJO that implements `Config` and declares JSR-380 constraints (e.g.
  `@NotBlank`, `@Min`) works — a violation fails fast with a readable message instead of a
  half-initialised plugin. See [Config Loading](examples/config-loading.md).
- **`BaseCommand`** — a thin base class that wraps a `CommandAPICommand` and implements the
  `Command` registration interface, so every command in every plugin is declared and registered
  the same way. See [Commands](examples/commands.md).
- **`DataStore`** — an asynchronous, write-behind key-value store split into namespaced
  `Repository` instances. Writes are buffered in memory and flushed to disk off the main thread.
  SQLite is bundled; MongoDB is a drop-in alternative behind the same API. Optional Bukkit helpers
  cover per-player data, periodic flushing, and `Location`/`ItemStack` serialization. Start with
  the [Data Store](examples/store.md) guide.

## How the pieces fit together

The three packages are independent — use one, two, or all of them:

| Package | Entry point | Guide |
|---------|-------------|-------|
| `config` | `ConfigManager` | [Config Loading](examples/config-loading.md) |
| `command` | `BaseCommand` | [Commands](examples/commands.md) |
| `store` | `DataStore` | [Data Store](examples/store.md) → [Bukkit Integration](examples/store-bukkit.md), [MongoDB](examples/store-mongo.md) |

## Documentation

- **[Getting Started](getting-started.md)** — add cw-commons as a dependency.
- **[Examples](examples/index.md)** — task-focused guides for config, commands, and storage.
- **[API Reference](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/)** —
  full Javadoc for the latest release, hosted by JitPack.
  [Older versions](https://jitpack.io/#CrimsonWarpedcraft/cw-commons) are kept too.
