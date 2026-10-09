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

1. **Configuration is TOML** at `<config-dir>/cellulosesz/cellulosesz.toml`.
2. **The default file is a static, commented template** packaged at
   `defaults/cellulosesz.toml`. It is written once on first boot and never re-encoded, so user
   comments survive. A test asserts the template decodes to the Kotlin defaults.
3. **No config `schemaVersion`.** New fields are added as defaults; unknown keys are rejected.
4. **Initial load is blocking and precedes feature use.** `ConfigStore` starts uninitialized;
   `current` throws until `initialLoadBlocking()` succeeds. A missing or invalid config fails
   startup rather than being silently repaired or defaulted.
5. **Reload is transactional.** A candidate is decoded and validated as a whole; transition
   validation runs before publishing. A failed reload keeps the previous snapshot and generation.
6. **`ConfigStore` is format-agnostic.** It depends on a `ConfigDecoder<T>`; `TomlConfigCodec`
   provides TOML via tomlkt. The generic store no longer hardcodes a serializer.
7. **Storage runtime changes are restart-required.** Transition validation recomputes the active
   `StorageConnectionSignature`; a change is reported as "restart required" instead of applying at
   runtime. Inactive backend sections may change freely.

## Consequences

- Only `application` knows the full config tree; each feature validates its own subtree.
- The database section is validated structurally for every backend, but only the active one is ever
  initialized or connected.
