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

## Next steps

- [Config Loading](examples/config-loading.md) — load and validate `config.yml`.
- [Commands](examples/commands.md) — register commands with `BaseCommand`.
- [Data Store](examples/store.md) — the write-behind key-value store (start here for storage).
- [API Reference](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/) —
  full Javadoc for the latest release.
