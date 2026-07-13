
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```powershell
# Full build: compiles, runs Checkstyle + SpotBugs, runs unit tests
./gradlew build          # macOS/Linux
gradlew.bat build        # Windows

# Tests only
./gradlew test

# Single test class
./gradlew test --tests "com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStoreTest"

# Integration tests (requires a pre-provisioned MongoDB service)
$env:CW_COMMONS_MONGO_URI = "mongodb://localhost:27017"
$env:CW_COMMONS_MONGO_DATABASE = "cw_commons_integration"
gradlew.bat integrationTest

# Release JAR + Javadoc jar (outputs build/libs/CwCommons.jar and CwCommons-<ver>-javadoc.jar)
./gradlew release -Pver=v1.0.0

# API docs only (outputs build/docs/javadoc/index.html)
./gradlew javadoc
```

`build` runs Checkstyle (Google Java Style, 100-char line limit, zero warnings), SpotBugs
(with FindSecBugs), and unit tests. Fix every warning — `maxWarnings = 0`. Integration tests are
an explicit task and are not part of `build` or `check`.

## Documentation site

The published docs site lives in `docs/` — a [Just the Docs](https://just-the-docs.com) Jekyll
site (landing page + the `docs/examples/` guides). `.github/workflows/docs.yml` builds and deploys
it to GitHub Pages (custom domain `cw-commons.crimsonwarpedcraft.com`) on each published release via
`actions/deploy-pages`; it skips prereleases.

The API reference (Javadoc) is hosted per-version by JitPack at
`https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/<version>/javadoc/` (the site links out
to `/latest/`). That works because the `shadow` Maven publication includes the `-javadoc.jar` built
by the `javadocJar` task — the same jar the `release` task drops in `build/libs/` and `release.yml`
attaches to the GitHub Release. The `javadoc` task options live in `build.gradle.kts`.

Preview locally (requires Ruby + Bundler):

```powershell
cd docs
bundle install                      # first run only
bundle exec jekyll serve            # http://localhost:4000

# Preview the API docs: open build/docs/javadoc/index.html
./gradlew javadoc
```

## Architecture

Java 25, Gradle 9.6.1 (Kotlin DSL, `java-library` plugin), distributed via JitPack as
`com.github.CrimsonWarpedcraft:cw-commons:VERSION`.

The packages below, each with its own responsibility:

**`config/`** — `ConfigManager` loads a YAML file via Jackson and validates it with Jakarta Bean
Validation (JSR-380). Any POJO implementing `Config` with constraint annotations works. It's a bare
holder built via `ConfigManager.builder()` (the domain constructor is package-private);
`config/bukkit/BukkitConfigManagerBuilder` is the standard entry point — it builds the default
YAML mapper + validator and registers `BukkitModule` so `Location`/`ItemStack` fields bind from YAML.
Advanced callers use `ConfigManager.builder().mapper(...).validator(...)`.

**`command/`** — `BaseCommand` wraps a pre-built `CommandAPICommand` and implements the `Command`
interface. CommandAPI is `compileOnly` and is NOT shaded into the library JAR.

**`store/`** — Write-behind key-value store:
- `DataStore` (public interface) → `ConcurrentDataStore` (package-private impl) manages a single-thread
  executor (`name + "-store-io"`) and a `RepositoryBuilder`. Build one via `DataStore.builder(backend)`
  (returns the public `DataStoreBuilder`; mutable fluent, required backend as the param).
  For Bukkit plugins (the common case) use `BukkitDataStoreBuilder` (in `store/bukkit/`), a
  `DataStoreBuilder` subclass that owns the full SQLite assembly.
- `RepositoryBuilder` (public `@FunctionalInterface`) → `ThreadedRepositoryBuilder`
  (public type, package-private constructors) wraps a `StorageBackend` and `Executor`, creating
  `ThreadedRepository` instances. `DataStoreBuilder.closeBackend(true)` (the default) makes
  `store.close()` close the backend (how `BukkitDataStoreBuilder` owns its `SqliteBackend`).
- `Repository<K,V>` (public interface) → `ThreadedRepository` (package-private impl) is a
  thin layer: serializes keys/values and dispatches all operations to the shared executor.
  No local state — caching lives in `CachingBackend`.
- `StorageBackend` (public interface): `SqliteBackend` (bundled, construct directly),
  `MongoDbBackend` (requires `org.mongodb:mongodb-driver-sync`; construct directly with
  `new MongoDbBackend(uri, databaseName)` — throws `IOException` if connection fails; pass it to
  `DataStore.builder(...)` for parity with SQLite), or `CachingBackend` (public type, package-private
  constructor; wraps any backend; owns per-namespace in-memory cache and write-buffering — applied
  automatically by `DataStoreBuilder`).
- `KeySerializer<K>` converts typed keys to `String`. Built-in factories: `KeySerializers.forUuid()`
  and `KeySerializers.forString()`.
- `CacheMode` is the builder-facing selector (`DataStoreBuilder.cacheMode(...)`): `CACHE_AND_FLUSH`
  (default) and `WRITE_THROUGH_ATOMIC` wrap the backend in a `CachingBackend` with the matching
  `WritePolicy`; `NONE` skips the `CachingBackend` entirely (raw backend, no read cache) for
  databases shared across servers. `WritePolicy` stays the required `CachingBackend` constructor arg.

**`bukkit/serialization/`** — shared Bukkit-specific Jackson (de)serializers for `Location` and
`ItemStack`, bundled by `BukkitModule` (a `SimpleModule` registering all four). Reused by both the
store and config Bukkit integrations. `LocationDeserializer` requires `world`/`x`/`y`/`z`;
`yaw`/`pitch` are optional and default to `0`. A stored world that isn't loaded deserializes to a
`Location` whose `world` is `null`.

**`store/bukkit/`** — store-side Bukkit glue:
- `BukkitDataStoreBuilder` (a `DataStoreBuilder` subclass; `new BukkitDataStoreBuilder(name, dataDir)`
  then `.build()`) is the recommended common-case entry point; it **owns** the full store assembly
  (SQLite backend + `BukkitModule` + lifecycle). Core `store/` stays Bukkit-free.
- `PlayerDataManager<V>` — wraps a `Repository<UUID, V>` and flushes on `PlayerQuitEvent`.
- `AutoFlushTask` — schedules periodic `DataStore.flush()` via `BukkitScheduler`; built via
  `AutoFlushTask.builder(store, plugin)` (`.interval()`, `.onFlush()`). `start()` returns a
  `BukkitTask` to cancel in `onDisable()`. The `onFlush` callback runs on the main thread after each
  flush (safe for Bukkit API calls).

**`config/bukkit/`** — config-side Bukkit glue: `BukkitConfigManagerBuilder` (a `ConfigManagerBuilder`
subclass) builds a `ConfigManager` with `BukkitModule` registered, so config classes can bind
`Location`/`ItemStack` fields from YAML.

## Test design rules

- Unit tests live in `src/test`, run via `test`/`build`, and must not depend on external services.
  Mock collaborators when the test is specifically checking delegation, caching, scheduling, or an
  error boundary.
- Integration tests live in `src/integrationTest` and run only via `integrationTest`. They exercise
  real SQLite files, Jackson YAML/JSON binding, Hibernate validation, and a configured MongoDB
  service through the public APIs.
- Integration tests never provision or administer external services. Running `integrationTest`
  requires `CW_COMMONS_MONGO_URI` and `CW_COMMONS_MONGO_DATABASE`; missing configuration fails the
  task. CI owns its MongoDB service lifecycle.
- MongoDB tests use unique collection names and only clean up collections created by that test.
  They must not create/drop databases, manage users, or start/stop MongoDB or Docker.
- Use `MockedStatic<Bukkit>` and related boundary mocks where Paper exposes only global/static
  lookups. Do not introduce MockBukkit unless the test truly requires a running server model.
- The MongoDB driver remains on `testImplementation` (not just `compileOnly`), which the integration
  source set inherits for runtime access.

## Shadow JAR

SQLite-JDBC is bundled in the shadow JAR but not relocated (native code requires stable paths).

Jackson, Hibernate Validator, and Jakarta Validation are declared as `api` deps — they are **not**
bundled. Consumers receive them transitively via JitPack and should relocate them when shading their
own plugin. Test code uses the original package paths.

## Checkstyle notes

- Google Java Style enforced. `@Test` methods are exempt from the `MethodName` check.
- Package-private classes do not require Javadoc.
- `VariableDeclarationUsageDistance` max is 3 — declare variables close to first use.
- Line limit is 100 characters. Wrap method chains with the `.` at the start of the continuation line.

## Windows / OneDrive quirk

IntelliJ may create read-only reparse-point placeholder directories under `build/`. If `gradlew build`
fails with an access-denied error, strip the ReadOnly attribute and force-delete the directory:

```powershell
attrib -R build /S /D
Remove-Item -Recurse -Force build
```
