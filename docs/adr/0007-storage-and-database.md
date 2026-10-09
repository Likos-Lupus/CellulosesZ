# ADR 0007: Storage and database

- Status: Accepted
- Date: 2026-10-09
- Supersedes the file-based machine-state persistence described in ADR 0002/0004/0005/0006.

## Context

Business state (homes, warps, spawn, teleport history, messaging preferences, mail, mutes,
moderation audit, kits, kit claims, player identities) was persisted as JSON files via
`AtomicFile`. This is workable but does not scale to remote/shared storage, offline identity, or
server migration. P0 replaces file machine state with JDBC and adds a cold-start endpoint migration.

## Decision

1. **Persistence is JDBC through HikariCP.** Business modules only see
   `foundation.database.DatabaseRuntime` (`read`/`transaction`); only `HikariDatabaseRuntime` sees
   Hikari types. Repositories never hold a `Connection` across suspension.
2. **Supported backends:** SQLite (default), H2, MySQL, MariaDB, PostgreSQL. Each has an independent
   `[database.<backend>]` TOML section; only `database.type` is initialized.
3. **SQLite defaults to a single-connection pool** (`maximum-pool-size = 1`) with
   `journal_mode=WAL`, `foreign_keys=ON`, `busy_timeout`, `synchronous=NORMAL`.
4. **No ORM, no Flyway/Liquibase.** The current schema is created with `CREATE TABLE IF NOT EXISTS`
   plus a compatibility probe (`SELECT ... WHERE 1 = 0`). There is no schema-version migration in
   this pre-release project.
5. **`schemaVersion` is removed from all machine data.** A row that fails domain validation raises a
   typed `*DataException` and is never silently repaired.
6. **Storage identifiers and signatures are distinct.**
    - `StorageIdentity` decides *where the data is* (backend, namespace, canonical path / host:
      port / database / schema). Changing it triggers a data migration.
    - `StorageConnectionSignature` (identity + username + secret fingerprint + SSL + pool) decides
      whether only the *connection* changed. A password or pool change reconnects without moving
      data.
7. **Endpoint changes migrate on cold start only.** The previous successful endpoint is recorded in
   `<world>/cellulosesz/.storage/last-successful.json`; the migration copies rows table-by-table in
   a single target transaction and records a `cz_storage_migration_journal` marker so a crash
   between the target commit and the sidecar rewrite can be finalized without recopying. The source
   is opened read-only and never modified or deleted. A non-empty target without a matching journal
   record is refused, never merged.
8. **No silent fallback.** If the active backend cannot connect, startup fails; it never falls back
   to SQLite.
9. **`/cellulosesz reload` never hot-swaps the pool.** A change to the active connection is rejected
   with "restart required"; changes to inactive sections or non-storage settings are allowed.
10. **Row identity is application-assigned** (UUIDs, names, compound keys). No auto-increment or
    sequence state, which keeps cross-database copies trivial. A `namespace` column isolates data so
    one database can be shared by several servers.

## Consequences

- The loader jars embed HikariCP, the five JDBC drivers, and the TOML parser as nested/Jar-in-Jar
  runtime dependencies (never flattened), so SQLite native resources and service metadata survive.
- SQLite/H2 repository contract tests run on every build; MySQL/MariaDB/PostgreSQL share the same
  repository code through a thin `SqlDialect`.
- Native account/IP bans remain vanilla's responsibility and are not mirrored into the database.
