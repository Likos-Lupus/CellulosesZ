# ADR 0009: Configuration

- Status: Accepted
- Date: 2026-10-09
- Supersedes the JSONC configuration described in ADR 0002.

## Context

The root config was `<config-dir>/cellulosesz/cellulosesz.jsonc`, decoded by a JSON `ConfigStore`
whose initial snapshot was the in-memory default and whose real load ran asynchronously after
`SERVER_STARTED`. Features therefore composed before their real configuration was loaded, and the
snapshot carried a `schemaVersion`.

## Decision

1. **Configuration is a directory of TOML files** at `<config-dir>/cellulosesz/`.
   `cellulosesz.toml` owns the foundational/operations scopes (`diagnostics`, `database`,
   `localization`, `text`, `commands`, `api`, `backup`); each feature module owns one file:
   `movement.toml`, `messaging.toml`, `administration.toml`, `utility.toml`, `players.toml`,
   `economy.toml`.
2. **A file is an independent unit.** Only the scopes registered for that file are decoded, so a
   table owned by another file is ignored rather than merged. An unknown top-level table is an
   error, and unknown keys inside an owned scope are still rejected. A missing file or table falls
   back to that scope's defaults.
3. **The default files are static, commented templates** packaged under `defaults/config/`. Each is
   written once on first boot and never re-encoded, so user comments survive. Tests assert that the
   templates compose to the Kotlin defaults and that every schema leaf is documented.
4. **No config `schemaVersion`.** New fields are added as defaults; unknown keys inside an owned
   scope are rejected.
5. **Initial load is blocking and precedes feature use.** `BundleConfigStore` starts uninitialized;
   `current` throws until `initialLoadBlocking()` succeeds. An invalid file fails startup; a missing
   file or table uses that scope's defaults (the templates are written on first boot, never during a
   reload).
6. **Reload is transactional.** Every file is decoded and validated before a candidate is published;
   transition validation runs before publishing. A failed reload keeps the previous snapshot and
   generation. A missing file is treated as a reset of its scopes, never rewritten during a reload.
7. **The store is format-agnostic.** `ConfigStore` (single file) and `BundleConfigStore` (one file
   per scope group) share the `ConfigHandle<T>` surface; both use `ConfigDecoder<T>` / tomlkt for
   TOML.
8. **Storage runtime changes are restart-required.** Transition validation recomputes the active
   `StorageConnectionSignature`; a change is reported as "restart required" instead of applying at
   runtime. Inactive backend sections may change freely.

## Consequences

- Only `application` knows the full config tree; each feature validates its own subtree.
- The database section is validated structurally for every backend, but only the active one is ever
  initialized or connected.
- Each file documents its own scope; a misplaced table is ignored and cannot silently override
  another feature.
