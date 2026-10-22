# ADR 0024 — Runtime JDBC driver provisioning

## Status

Accepted. Supersedes the "loader jars embed the five JDBC drivers" consequence of ADR 0007.

## Context

The mod supports five JDBC backends (SQLite, H2, MySQL, MariaDB, PostgreSQL). Packaging all five
drivers as nested/Jar-in-Jar dependencies made the distribution 26 MB, of which ~19 MB (72 %) were
drivers; a server only ever uses one of them.

Neither Fabric nor NeoForge offers a way for a mod to fetch a dependency at runtime: the documented
options are Loom `include` (nested jars) and NeoForge `jarJar`, both evaluated at build time, and
`fabric.mod.json` only accepts jar paths *inside* the mod jar. `FabricLoader` has no public
"add a mod/class path" API either. Mods therefore bundle their drivers (for example EasyAuth, whose
published jar is 23 MB for that reason).

The plugin ecosystem solves this differently and has done so for years: Paper/Spigot resolve a
`libraries:` list from Maven Central at startup (`LibraryLoader`, `MavenLibraryResolver`, with
`PAPER_DEFAULT_CENTRAL_REPOSITORY` to point at a mirror), and LuckPerms downloads the driver for the
configured storage type into `<plugin data>/libs/`, pins each artifact version and SHA-1, and loads
it through an isolated class loader (including the SQLite and H2 drivers).

## Decision

1. **No JDBC driver ships inside the mod jar.** The jar carries a generated manifest
   (`cellulosesz/drivers/drivers.properties`) with the coordinate, file name, size, SHA-256 and
   Maven path of each backend's driver, produced from `gradle/libs.versions.toml` by
   `generateDriverManifest`. Versions and hashes therefore cannot drift from the build.
2. **Resolution order, evaluated once per cold start:** mod class loader (a development run, or a
   driver another mod ships) → verified file in `<game dir>/cellulosesz/drivers/` → download from
   `[database.driver] repository`.
3. **`DriverManager` is not used.** It only accepts drivers visible to the *caller's* class loader,
   so a downloaded jar would be invisible to HikariCP. The driver is instantiated from a child
   `URLClassLoader` and wrapped in a `javax.sql.DataSource` handed to `HikariConfig.dataSource`.
   HikariCP only builds its own `DriverDataSource` when `dataSource` is unset.
4. **Only the active backend is provisioned — plus the previous endpoint when a migration is
   pending.** `StorageBootstrap` resolves the recorded `last-successful` endpoint as the migration
   source, so a switch from MySQL to SQLite needs both drivers.
5. **Checksums are mandatory.** A download is streamed to a temporary file, verified against the
   pinned size and SHA-256, and only then atomically moved into place. A local file that fails
   verification is quarantined as `<name>.corrupt-<timestamp>`; it is never silently deleted and
   never used.
6. **Failure is fatal.** A backend whose driver cannot be obtained stops the cold start with the
   expected path, source URL, checksum and the two remedies (`place the jar`, `enable
   auto-download`). There is no fallback to another backend and no silent degradation.
7. **Offline escape hatch.** `auto-download = false` disables network access entirely. The
   `packageDriverBundle` build task publishes the same jars as one archive
   (`cellulosesz-drivers-<version>.zip`) that unpacks into `<game dir>/cellulosesz/drivers/`.
8. **Provisioning runs during the cold start**, in `StorageBootstrap.startBlocking`, before the
   connection pool exists. This is the one documented exception to "no file I/O on the server
   thread": it happens once, before the first tick, on the same thread that already performs the
   JDBC connection and schema work, and it never runs while the server is ticking.
9. `[database.driver]` changes are restart-bound, like the active connection itself.

## Consequences

- The distribution shrinks to ~8 MB (Fabric) / ~12 MB (NeoForge, Kotlin runtime included).
- A first start on a fresh host needs to fetch one driver (0.8–12 MB) unless the offline bundle is
  unpacked first; a firewall that blocks Maven Central turns into an explicit, actionable error
  instead of a silent failure.
- Driver upgrades no longer require a mod release; the pinned version stays in the version catalog,
  and an operator may pin a different patch by placing a jar in the driver directory.
- The mod executes a downloaded jar, so the download path is pinned to `https`/`file`, checksummed
  and opt-out. There is no transitive resolution and no metadata trust.
- SQLite's 12 MB cross-platform native payload is downloaded instead of shipped; platforms whose
  natives are missing from the jar are unaffected because the upstream jar is used verbatim.
