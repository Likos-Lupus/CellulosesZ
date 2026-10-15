# ADR 0014 — Command route ownership and conflict handling

- **Status**: Accepted
- **Date**: 2026-10-16
- **Supersedes**: none
- **Related**: ADR 0011 (command DSL and help), ADR 0015 (reload and runtime configuration)

## Context

`BrigadierCompiler` used to call `dispatcher.register(builder)` for a definition's canonical name
and every alias unconditionally. Brigadier merges same-named root nodes, so when vanilla or another
mod already owned a literal (for example `/tp`, `/kill`, `/help`), CellulosesZ silently merged into
the existing node and could end up sharing an argument type or executor with code it did not
control. `RegisteredCommands.ownerOf(name)` then reported the name as "owned" purely because it was
declared, which is not ownership at all.

The mod must coexist with third-party mods that register overlapping command names, without silently
hijacking them, and it must still expose a deterministic entry point of its own.

## Decision

1. **Plan, decide, then install.** `CommandRoutePlanner` (pure) turns the declarations and an
   immutable `CommandRoutingPolicy` into an ordered plan: admin root, then each definition's
   mandatory `cellulosesz:<name>` route, then optional canonical short names, then sorted aliases.
2. **Inspect before writing.** `CommandRegistry` reads `dispatcher.root.getChild(literal)` *before*
   any write. A free literal is built by `BrigadierCompiler.buildRoot` and added; an occupied one is
   never merged into.
3. **Required vs optional routes.** The admin root (`/cellulosesz`) and the namespaced route
   (`cellulosesz:<name>`) are required: a conflict is recorded as `INSTALL_MISMATCH` and logged at
   ERROR. Short names and aliases are optional: a conflict is `SKIPPED_ALREADY_PRESENT`, and the
   namespaced fallback stays available. We deliberately do **not** crash a running server for a
   compatibility conflict, and we never remove or replace a foreign node.
4. **Verify the install.** After `addChild`, the registry checks `getChild(literal) === node`; any
   other node means `INSTALL_MISMATCH`, never a claim of ownership.
5. **Report, do not guess.** Each pass returns a Brigadier-free `CommandRegistrationReport` keyed to
   a monotonic generation. It carries only literals, route kinds, statuses and the namespaced
   fallback — never a node, dispatcher, player, argument value or permission detail. The composition
   root keeps the latest report and feeds it to `/cellulosesz commands` and to help ownership.
6. **Idempotent re-registration.** Nodes installed for the current dispatcher are remembered by
   identity, so a registration event that fires twice on the same dispatcher is a no-op rather than
   a false conflict. A new dispatcher starts a fresh generation with no retained node.
7. **Help follows effective routes.** `CommandHelpResolver` only resolves a query whose first token
   CellulosesZ actually installed in the current generation; a declared-but-skipped name keeps the
   vanilla/external document.

Namespaced literals are validated by a dedicated parser (fixed `cellulosesz:` prefix plus the DSL's
`[a-z0-9_-]+` local part); a literal is never accepted merely because it contains a colon.

## Consequences

- The short semantics of `/tp`, `/kill`, `/gamemode` change: they are vanilla-owned unless free.
  Operators move scripts and command blocks to `/cellulosesz:<command>`; this is a user-visible
  change recorded in the changelog, not a transparent refactor.
- `/me` ownership is decided by `MeRouteMode`; see ADR 0015.
- The stable fallback is `cellulosesz:<name>`. Its reachability under both loaders must be confirmed
  by an integration smoke test (open decision D-01); if a loader rejects the namespaced form, the
  fallback becomes `/cellulosesz <name>`.
- `RegisteredCommands`/`ownerOf` are removed; `CommandRegistrationReport.owns(literal)` replaces the
  false ownership claim.
