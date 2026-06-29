---
title: Getting Started
nav_order: 2
---

# Getting Started

cw-commons is published via [JitPack](https://jitpack.io). Add the repository and the dependency to
your plugin's build script.

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.CrimsonWarpedcraft:cw-commons:VERSION")
}
```

Replace `VERSION` with a release tag (e.g. `v1.0.0`) or `main-SNAPSHOT` for the latest unreleased
build from `main`. Browse the available versions on the
[JitPack page](https://jitpack.io/#CrimsonWarpedcraft/cw-commons).

cw-commons targets **Java 25** and is built against the Paper API, so it runs on any Paper-compatible
server.

## Transitive dependencies

These come along automatically — you do **not** declare them yourself:

| Dependency | Purpose |
|------------|---------|
| Jackson (databind + YAML) | JSON/YAML serialization for `ConfigManager` and the data store |
| Hibernate Validator + Jakarta Validation | constraint checking in `ConfigManager` |

SQLite-JDBC is bundled **inside** the cw-commons JAR (and not relocated, because its native code
needs stable package paths), so the [SQLite data store](examples/store.md) works out of the box
with no extra dependency.

> If you shade your plugin, relocate Jackson, Hibernate Validator, and Jakarta Validation under your
> own namespace to avoid clashing with other plugins on the same server. cw-commons declares them as
> `api` dependencies precisely so you receive them transitively and control the relocation.

## Optional dependencies

Some features need a dependency you add yourself:

- **CommandAPI** — required by [`BaseCommand`](examples/commands.md). It is `compileOnly` in
  cw-commons and **not** shaded into the JAR; shade it into your own plugin as usual.
- **MongoDB driver** — required only if you use [`MongoDbBackend`](examples/store-mongo.md). Add
  `org.mongodb:mongodb-driver-sync` to your plugin.

## How you construct things

cw-commons follows one rule, applied by type shape:

- **Builders** for assembled, configurable types. A static `builder(...)` takes the **required**
  collaborators as parameters (so the compiler enforces them); fluent setters cover the optional
  knobs, and `build()` produces the object:
  - [`DataStore.builder(backend)`](examples/store.md#advanced-custom-backends-and-serialization),
    or `new BukkitDataStoreBuilder(name, dataFolder)` for the Bukkit common case.
  - [`ConfigManager.builder()`](examples/config-loading.md), or `new BukkitConfigManagerBuilder()`
    for the Bukkit common case.
  - [`AutoFlushTask.builder(store, plugin)`](examples/store-bukkit.md#periodic-flushing-with-autoflushtask).
- **Plain constructors** for required-only leaf/value types: `new SqliteBackend(file)`,
  `new MongoDbBackend(uri, db)`, `new PlayerDataManager<>(repo, plugin)`, `new BaseCommand(...)`.
- **Static factories** for canned strategy objects: `KeySerializers.forUuid()` / `forString()`.
- **No-arg constructors** for framework glue: `new BukkitModule()` and the Jackson (de)serializers.

> **Migrating from 0.1.x.** The old static factories became builders:
> `BukkitDataStores.getLocalDataStore(name, dir)` → `new BukkitDataStoreBuilder(name, dir).build()`;
> `BukkitConfigManagers.create()` → `new BukkitConfigManagerBuilder().build()`; and
> `new AutoFlushTask(store, plugin, ticks, cb)` →
> `AutoFlushTask.builder(store, plugin).interval(ticks).onFlush(cb).build()`. The
> `ConcurrentDataStore`, `CachingBackend`, and `ThreadedRepositoryBuilder` constructors are now
> internal — assemble through `DataStore.builder(backend)` instead, which also gives MongoDB the same
> one-builder path as SQLite.

## Next steps

- [Config Loading](examples/config-loading.md) — load and validate `config.yml`.
- [Commands](examples/commands.md) — register commands with `BaseCommand`.
- [Data Store](examples/store.md) — the write-behind key-value store (start here for storage).
- [API Reference](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/) —
  full Javadoc for the latest release.
