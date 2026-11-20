---
title: Release Notes
nav_order: 4
---

# Release notes

## v0.4.0

**Baseline:** Java 25 and Paper 26.3. For available tags, see the
[published releases](https://github.com/CrimsonWarpedcraft/cw-commons/releases).

### Upgrading from v0.3.4

- **Close your config manager after its last use.** `ConfigManager` implements `AutoCloseable`.
  Use try-with-resources for a one-time load; the loaded config remains usable. If your plugin
  reloads config, keep the manager until `onDisable()`. If you supply a validator, you still own
  its factory. See [manager lifetime](examples/config-loading.md#manager-lifetime).
- **Use `update` for counters and other read/modify/write operations.** Separate `get` and `put`
  calls can lose an increment. `Repository.update` and `PlayerDataManager.update` serialize the
  whole change within one store instance. With buffered writes, the future acknowledges the cached
  value; a successful flush persists it. Custom repositories remain compatible: if they do not
  implement `update`, its future fails with `UnsupportedOperationException`. See
  [atomic updates](examples/store.md#atomic-updates).
- **Check hand-written location values.** Coordinates must be finite; quoted decimals still work.
  A typo such as `x: potato` now fails binding instead of placing the location at zero. Worlds
  remain optional, and omitted or null `yaw`/`pitch` still default to zero unless the field has
  `@RequireOrientation`. Empty, null, malformed or wrong-shaped config documents fail with
  `IOException` identifying the file; constraint violations remain `IllegalStateException`.

### Storage and packaging

- Closing a store rejects new requests, drains accepted work, and flushes before releasing owned
  resources. Failed write-through saves keep the previous cached value; a failed buffered flush
  leaves dirty entries available for retry. See [store lifecycle](examples/store.md#flushing-and-lifecycle).
- Bukkit's periodic and player-quit flushes log failures. The periodic success callback is skipped
  if the plugin is disabled, and flush intervals must be positive.
- Failed backend initialization cleans up acquired resources. Constructing the MongoDB backend
  still does not verify connectivity or credentials; failures can surface on the first operation.
- The commons JAR bundles SQLite JDBC, including its service provider and native libraries.
  Jackson YAML and Hibernate Validator remain Maven compile dependencies. CI checks the published
  JAR and POM and tests a shaded consumer plugin on Ubuntu and Windows.
