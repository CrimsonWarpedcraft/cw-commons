
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```powershell
# Full build: compiles, runs Checkstyle + SpotBugs, runs all tests
./gradlew build          # macOS/Linux
gradlew.bat build        # Windows

# Tests only
./gradlew test

# Single test class
./gradlew test --tests "com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStoreTest"

# Release JAR (outputs build/libs/CwCommons.jar)
./gradlew release -Pver=v1.0.0
```

`build` runs Checkstyle (Google Java Style, 100-char line limit, zero warnings) and SpotBugs
(with FindSecBugs) in addition to tests. Fix every warning — `maxWarnings = 0`.

## Architecture

Java 25, Gradle 9.5.1 (Kotlin DSL, `java-library` plugin), distributed via JitPack as
`com.github.CrimsonWarpedcraft:cw-commons:VERSION`.

Three packages, each with its own responsibility:

**`config/`** — `ConfigManager` loads a YAML file via Jackson and validates it with Jakarta Bean
Validation (JSR-380). Any POJO implementing `Config` with constraint annotations works. The
package-private `ConfigManager(ObjectMapper, Validator)` constructor is a test seam.

**`command/`** — `BaseCommand` wraps a pre-built `CommandAPICommand` and implements the `Command`
interface. CommandAPI is `compileOnly` and is NOT shaded into the library JAR.

**`store/`** — Write-behind key-value store:
- `DataStore` (public interface) → `ConcurrentDataStore` (public impl) manages a single-thread
  executor (`name + "-store-io"`) and a `RepositoryBuilder`.
  Use `DataStore.getLocalDataStore(name, dataDir)` as the primary entry point (SQLite-backed,
  owns the backend lifecycle). For custom backends, construct `ConcurrentDataStore` directly.
- `RepositoryBuilder` (public `@FunctionalInterface`) → `ThreadedRepositoryBuilder`
  (public impl) wraps a `StorageBackend` and `Executor`, creating `ThreadedRepository` instances.
- `Repository<K,V>` (public interface) → `ThreadedRepository` (package-private impl) is a
  thin layer: serializes keys/values and dispatches all operations to the shared executor.
  No local state — caching lives in `CachingBackend`.
- `StorageBackend` (public interface): `SqliteBackend` (bundled, construct directly),
  `MongoDbBackend` (requires `org.mongodb:mongodb-driver-sync`; construct directly with
  `new MongoDbBackend(uri, databaseName)` — throws `IOException` if connection fails), or
  `CachingBackend` (public decorator; wraps any backend; owns per-namespace in-memory cache
  and write-buffering).
- `KeySerializer<K>` converts typed keys to `String`. Built-in factories: `KeySerializers.forUuid()`
  and `KeySerializers.forString()`.
- `WritePolicy.CACHE_AND_FLUSH` (default) buffers writes; `WRITE_THROUGH_ATOMIC` writes immediately.

**`store/bukkit/`** — Bukkit-specific Jackson serializers/deserializers for `Location` and
`ItemStack`, plus:
- `PlayerDataManager<V>` — wraps a `Repository<UUID, V>` and flushes on `PlayerQuitEvent`.
- `AutoFlushTask` — schedules periodic `DataStore.flush()` via `BukkitScheduler`; `start()`
  returns a `BukkitTask` to cancel in `onDisable()`. Optional `Runnable onFlush` callback runs
  on the main thread after each flush (safe for Bukkit API calls).

## Test design rules

- **No integration tests.** Real I/O is only allowed inside `SqliteBackendTest` (in-memory SQLite,
  plus one `@TempDir`-backed store creation test).
  Every other test mocks `StorageBackend` with `mock(StorageBackend.class)`.
- Mock `StorageBackend` and stub `load`/`loadAll` to return `Optional.empty()` / `new HashMap<>()`
  in `@BeforeEach`. `CachingBackendTest` calls `CachingBackend` directly (no executor needed).
  `ThreadedRepositoryTest` constructs `ThreadedRepository` directly with `Runnable::run`.
- Use `MockedStatic<Bukkit>` and `MockedStatic<ItemStack>` for static calls in the bukkit package,
  and `MockedStatic<MongoClients>` in `MongoDbBackendTest` to intercept `MongoClients.create()`
  during construction. Requires the Mockito inline agent already configured in `build.gradle.kts`.
- MongoDB driver is on `testImplementation` (not just `compileOnly`) to enable static mocking.

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
