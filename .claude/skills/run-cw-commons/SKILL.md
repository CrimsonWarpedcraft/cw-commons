---
name: run-cw-commons
description: Build, test, and release cw-commons from source. Use when asked to build the library, run its tests, run a single test class, or produce a release JAR.
---

`cw-commons` is a Java library — "running" it means building from source, executing the test
suite, and optionally producing the shadow JAR for distribution via JitPack.

## Build

```powershell
gradlew.bat build          # Windows
./gradlew build            # macOS / Linux
```

Runs compilation, Checkstyle (Google Java Style, 100-char limit, `maxWarnings = 0`), SpotBugs
with FindSecBugs, and unit tests. Fix every warning — the build fails on the first one.

## Verify

```powershell
gradlew.bat test
```

All unit tests pass. Subset by class:

```powershell
gradlew.bat test --tests "com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStoreTest"
```

Integration tests are manual and require an externally provisioned MongoDB service:

```powershell
$env:CW_COMMONS_MONGO_URI = "mongodb://localhost:27017"
$env:CW_COMMONS_MONGO_DATABASE = "cw_commons_integration"
gradlew.bat integrationTest
```

The tests must not start/stop MongoDB or Docker, create/drop databases, or manage users. They may
create and clean up their own uniquely named collections.

## Release JAR

```powershell
gradlew.bat release "-Pver=v1.0.0"
# → build/libs/CwCommons.jar  (~16.6 MB shadow JAR)
```

The `-Pver` argument must be quoted on Windows to prevent PowerShell from parsing `=` as an
operator. Output is a relocating shadow JAR (shades Jackson, SnakeYAML, Hibernate Validator).

## Maintenance

After adding a dependency, scan `build.gradle.kts` for outdated versions of all other
dependencies and update them if safe. Run `gradlew.bat build` to confirm.

Keep these in sync with the current state of the project:

- **`CLAUDE.md`** — architecture, shadow JAR relocation table, test design rules
- **`README.md`** — usage examples, dependency snippets
- **`docs/examples/`** — `store.md` (data store concepts + SQLite), `store-mongo.md`,
  `store-bukkit.md` (player data, auto-flush, Bukkit serializers), `config-loading.md`,
  `commands.md`; update any code snippets that reference changed APIs
- **`.claude/skills/run-cw-commons/SKILL.md`** (this file) — build commands, shaded package
  list, JAR size estimate

## Gotchas

- **`release "-Pver=..."` quoting** — on Windows PowerShell, omitting the quotes around
  `-Pver=v1.0.0` silently passes a malformed property; always quote it as shown above.
- **`build/` access-denied on Windows/OneDrive** — IntelliJ can leave read-only reparse-point
  placeholder directories under `build/`. If `gradlew build` fails with access-denied, strip
  the attribute and force-delete:
  ```powershell
  attrib -R build /S /D
  Remove-Item -Recurse -Force build
  ```
- **`@TempDir` file-lock on Windows** — `SqliteDataStoreIntegrationTest` relies on the full close chain
  (`DataStore.close()` → `CachingBackend.close()` → `SqliteBackend.close()`) releasing the
  SQLite file lock before `@TempDir` cleanup runs. A failing test here means the close chain
  is broken, not a test framework issue.
- **MongoDB driver on `testImplementation`** — the integration source set inherits this
  configuration so `MongoDataStoreIntegrationTest` can connect to the configured service.
