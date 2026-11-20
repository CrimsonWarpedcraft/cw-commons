# Contributing to CwCommons

Thanks for your interest in improving CwCommons! Bug reports, feature ideas, docs fixes, and code
contributions are all welcome — you don't need commit access to get started.

## Getting help

- 💬 **[Discord](https://discord.gg/5XMmeV6EtJ)** — the fastest way to ask a question or talk
  through an idea before you start.
- 📖 **[Documentation](https://cw-commons.crimsonwarpedcraft.com)** — usage guides and the API
  reference.
- 🏗️ **[`CLAUDE.md`](CLAUDE.md)** — a tour of the architecture, packages, build commands, and the
  test-design rules. Read this before making non-trivial changes.

## Workflow

0. **(External contributors only)** Create a fork of the repository.
1. Pull any changes from `main` so you're up to date.
2. Create a branch from `main`.
   - Give it a name that describes the change (e.g. `add-scoreboard`).
   - Focus on one change per branch.
3. Commit your changes.
   - Keep commits small and focused.
   - Write commit messages in [Conventional Commit](https://www.conventionalcommits.org/) format.
4. When you're ready, open a pull request to `main`.
   - Keep PRs small (preferably < 300 lines changed).
   - Format the PR **title** in Conventional Commit format too — it feeds the release notes.
   - Describe what changed in the body, and link any related issues.

After the pull request is reviewed, approved, and passes all automated checks, it will be merged
into `main`.

### Conventional Commit example

```text
feat(store): Add MongoDB backend for the data store

ADDED   - MongoDbBackend behind the existing StorageBackend interface
CHANGED - DataStore.builder(...) now accepts any StorageBackend
```

Common scopes mirror the package layout: `config`, `command`, `store`, `bukkit`, plus `deps`,
`docs`, and `ci`.

## Building & checks

Use an installed **JDK 25** and the checked-in **Gradle 9.9.1** wrapper. The build selects a Java 25
toolchain for compilation, tests, and Javadoc and targets Java 25 bytecode. Dependency/plugin pins
are centralized in `gradle/libs.versions.toml`; update shared versions there so test, integration,
and Mockito agent dependencies stay aligned.

```bash
./gradlew build        # macOS / Linux / Unix
gradlew.bat build      # Windows
```

`build` compiles the project, runs **Checkstyle** (Google Java Style, 100-character lines),
**SpotBugs + FindSecBugs**, and the **JUnit unit test** suite. The build is configured with
`maxWarnings = 0`, so **every warning must be fixed** before a PR can merge.

For Windows agent terminals, these are task-selection examples: use the
[bounded, log-redirected child-process procedure in `CLAUDE.md`](CLAUDE.md#windows-agent-terminal)
for every Gradle invocation, with `JAVA_HOME` pointing to JDK 25.

```bash
./gradlew test                                                   # tests only
./gradlew test --tests "com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStoreTest"  # one class
./gradlew generatePomFileForShadowPublication javadoc             # local publication/docs checks
./gradlew release -Pver=v1.0.0                                   # build the release + javadoc jars
```

Integration tests are a separate, explicit task. Before running them, provision a MongoDB instance
and grant the configured database permission to create and drop test collections. The tests do not
start services, manage Docker, create/drop databases, or manage users.

```bash
# macOS / Linux
export CW_COMMONS_MONGO_URI=mongodb://localhost:27017
export CW_COMMONS_MONGO_DATABASE=cw_commons_integration
./gradlew integrationTest
```

```powershell
# Windows PowerShell
$env:CW_COMMONS_MONGO_URI = "mongodb://localhost:27017"
$env:CW_COMMONS_MONGO_DATABASE = "cw_commons_integration"
gradlew.bat integrationTest
```

Invoking `integrationTest` without both variables is an error. CI provisions MongoDB outside the
test process and supplies these variables automatically.

> **Windows / OneDrive:** if `gradlew build` fails with an access-denied error on `build/`, see the
> workaround in [`CLAUDE.md`](CLAUDE.md#windows--onedrive-quirk).

### Verify a Maven candidate against the plugin template

The Ubuntu/Windows build checks stage the real `shadow` Maven publication in a fresh repository.
They check the JAR (including multi-release classes, JDBC service and native libraries), its
Javadoc artifact and the exact compile dependencies in the POM. Then the pinned template runs its
unit, integration and isolated shaded-JAR tests. This check uses local SQLite and needs no server.

To reproduce with JDK 25 and Python 3.11+ (from the commons root, Bash):

```bash
repo=$(mktemp -d)
version=0.0.0-CI-LOCAL
publish=(publishToMavenLocal "-Dmaven.repo.local=$repo" "-Pver=v$version")
./gradlew "${publish[@]}"
./gradlew "${publish[@]}" -I ci/jitpack-group.init.gradle
python3 ci/verify_publication.py "$repo" "$version"

# Set template to a checkout of the commit pinned in .github/actions/verify-publication/action.yml.
"$template/gradlew" -p "$template" -I "$PWD/ci/template-candidate.init.gradle" \
  "-PcandidateRepo=$repo" "-PcandidateVersion=$version" \
  verifyCandidateResolution build integrationTest artifactTest --no-build-cache
```

The second publish uses the same artifacts and POM generation under JitPack's consumer group.
This matters because the template's Shadow minimization rules match the group and module name.
The consumer init script restricts that module to the temporary repository, selects the candidate,
and checks the resolved bytes against the native publication. No template edits are required.

## Code & test conventions

- **Javadoc** is required on public API; package-private classes are exempt.
- Keep unit tests isolated from external services. Put real SQLite, MongoDB, Jackson file-binding,
  and cross-component coverage in `src/integrationTest`.
- Integration tests may create and clean up their own tables, collections, and documents, but must
  not own the database server, container, database, or user lifecycle.
- Use `MockedStatic` only at unavoidable Bukkit/static boundaries; integration tests do not require
  MockBukkit.
- See the **Test design rules** and **Checkstyle notes** sections of [`CLAUDE.md`](CLAUDE.md) for
  the full detail — please skim them before adding tests.

## Building the documentation

The site at [cw-commons.crimsonwarpedcraft.com](https://cw-commons.crimsonwarpedcraft.com) is a
[Just the Docs](https://just-the-docs.com) Jekyll site under `docs/`. `.github/workflows/docs.yml`
deploys it on pushes to `main` that change `docs/**` or the docs workflow, and on manual dispatch,
not on releases. The API reference (Javadoc) is hosted per-version by
[JitPack](https://jitpack.io/#CrimsonWarpedcraft/cw-commons), which the site links out to.

```bash
# Preview the site locally (requires Ruby + Bundler). First run only: bundle install
cd docs
bundle install
bundle exec jekyll serve   # http://localhost:4000

# Preview the API docs locally — open build/docs/javadoc/index.html
cd ..                     # back to the repository root
./gradlew javadoc
```

## Reporting bugs & requesting features

Open an issue using one of the templates:

- [🐛 Bug report](https://github.com/CrimsonWarpedcraft/cw-commons/issues/new?template=bug_report.md)
- [✨ Feature request](https://github.com/CrimsonWarpedcraft/cw-commons/issues/new?template=feature_request.md)

Please include version information (Minecraft, server, and library versions) and any console output
when reporting a bug.
