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

```bash
./gradlew build        # macOS / Linux / Unix
gradlew.bat build      # Windows
```

`build` compiles the project, runs **Checkstyle** (Google Java Style, 100-character lines),
**SpotBugs + FindSecBugs**, and the **JUnit** test suite. The build is configured with
`maxWarnings = 0`, so **every warning must be fixed** before a PR can merge.

```bash
./gradlew test                                                   # tests only
./gradlew test --tests "com.crimsonwarpedcraft.cwcommons.store.ConcurrentDataStoreTest"  # one class
./gradlew release -Pver=v1.0.0                                   # build the release + javadoc jars
```

> **Windows / OneDrive:** if `gradlew build` fails with an access-denied error on `build/`, see the
> workaround in [`CLAUDE.md`](CLAUDE.md#windows--onedrive-quirk).

## Code & test conventions

- **Javadoc** is required on public API; package-private classes are exempt.
- **No integration tests.** Mock `StorageBackend` with `mock(StorageBackend.class)`; real I/O is
  only allowed inside `SqliteBackendTest`. Use `MockedStatic` for Bukkit/static calls.
- See the **Test design rules** and **Checkstyle notes** sections of [`CLAUDE.md`](CLAUDE.md) for
  the full detail — please skim them before adding tests.

## Building the documentation

The site at [cw-commons.crimsonwarpedcraft.com](https://cw-commons.crimsonwarpedcraft.com) is a
[Just the Docs](https://just-the-docs.com) Jekyll site under `docs/`, published on each release by
`.github/workflows/docs.yml`. The API reference (Javadoc) is hosted per-version by
[JitPack](https://jitpack.io/#CrimsonWarpedcraft/cw-commons), which the site links out to.

```bash
# Preview the site locally (requires Ruby + Bundler). First run only: bundle install
cd docs
bundle install
bundle exec jekyll serve   # http://localhost:4000

# Preview the API docs locally — open build/docs/javadoc/index.html
./gradlew javadoc
```

## Reporting bugs & requesting features

Open an issue using one of the templates:

- [🐛 Bug report](https://github.com/CrimsonWarpedcraft/cw-commons/issues/new?template=bug_report.md)
- [✨ Feature request](https://github.com/CrimsonWarpedcraft/cw-commons/issues/new?template=feature_request.md)

Please include version information (Minecraft, server, and library versions) and any console output
when reporting a bug.
