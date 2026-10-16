# ADR 0015 — Reload and runtime configuration

- **Status**: Accepted
- **Date**: 2026-10-16
- **Related**: ADR 0009 (configuration), ADR 0014 (command route ownership)

## Context

`/cellulosesz reload` decoded and validated a candidate, validated the database transition,
published it, and then ran a single follow-up (`applyNicknameDisplay`). A reload could therefore
report success while a registration-time binding had silently not changed: `/me` ownership, the
command short-name flags, and the jail command allow-list were baked in when the dispatcher or the
vanilla command tree was built.

## Decision

Configuration changes are classified by lifecycle:

| Kind                   | Examples                                                                          | Reload behaviour                                                                         |
|------------------------|-----------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `STATIC_BINDING`       | `[commands]`, effective `/me` ownership, `/help` overlay switch                   | **Atomic reject** with a "restart required" error; the previous snapshot stays in effect |
| `RUNTIME_POLICY`       | `players.nicknames.applyToVanillaDisplay`, `administration.jail.restrictCommands` | Applied after the snapshot is published, on the server thread                            |
| `RUNTIME_READ_THROUGH` | most `settings()` reads                                                           | Nothing to do                                                                            |
| `STORAGE_CONNECTION`   | active `[database]` endpoint/pool/credentials                                     | Existing restart-required reject                                                         |

1. `ConfigValidation.rootTransition` composes the existing database check with a new
   `validateCommandTransition`. It compares the whole `[commands]` block (including the
   `[commands.vanillaOverrides]` allowlist) and the action-message `enabled` flag, since both decide
   command registration; any change is rejected as a startup binding.
2. On a successful reload the composition root runs `applyRuntimeSettings` on the **server thread**
   via `RuntimeKernel.onServerThread`: refresh the nickname display flag and the jail restriction
   snapshot. A failure of either is not possible by construction (both are atomic volatile snapshot
   assignments), so the success message is never sent ahead of the state change.
3. Command routing is never re-planned in place: `/me` and the short-name flags are startup
   bindings.

## Consequences

- A reload that changes a static binding fails loudly and names the field; the running command tree
  is untouched.
- Dynamic policy changes take effect immediately with no restart.
- The configuration template documents `[commands]` as restart-required.
