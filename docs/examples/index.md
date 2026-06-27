---
title: Examples
nav_order: 3
has_children: true
---

# Examples

Task-focused guides for the main features of cw-commons. Each page is a self-contained walkthrough
with copy-pasteable code.

## Configuration & commands

- [Config Loading](config-loading.md) — load and validate a YAML config with `ConfigManager`.
- [Commands](commands.md) — register commands with `BaseCommand`.

## Data storage

Start with [Data Store](store.md); the other two build on it but not on each other, so read
whichever applies to you (and skip MongoDB entirely if you don't need it):

1. [Data Store](store.md) — the concepts behind the write-behind key-value store, plus the bundled
   SQLite backend. **Start here.**
2. [Bukkit Integration](store-bukkit.md) — per-player data, periodic flushing, and storing Bukkit
   types like `Location` and `ItemStack`.
3. [MongoDB Backend](store-mongo.md) — *optional.* The same `DataStore` API, backed by MongoDB
   instead of a local file.

For the full API, see the
[Javadoc](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/).
