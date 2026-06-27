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
build from `main`.

## Transitive dependencies

Jackson, Hibernate Validator, and Jakarta Validation are provided as transitive dependencies — no
need to declare them separately. If you shade your plugin, include and relocate them under your own
namespace to avoid conflicts with other plugins on the same server.

CommandAPI is **not** included — shade it in your own plugin as you normally would, and it will be
compatible with `BaseCommand`. The MongoDB driver is likewise optional; add
`org.mongodb:mongodb-driver-sync` only if you use `MongoDbBackend`.

## Next steps

Head to the [Examples](examples/index.md) for task-focused guides, or jump straight to the
[API Reference](https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/latest/javadoc/).
