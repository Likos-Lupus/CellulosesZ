# ADR 0018 — Teleport compatibility hooks

- **Status**: Accepted
- **Date**: 2026-10-16
- **Related**: ADR 0003 (teleportation), ADR 0014 (command route ownership)

## Context

A final teleport veto (for example a land-claim mod) and a confirmed-success notification are the
two extension points a third-party integration could need. ADR 0003 already rejected a general
multi-stage middleware framework; we do not reintroduce one.

## Decision

Two narrow, constructor-injected ports live in `movement`:

- `TeleportPreflight` — a synchronous `fun interface` invoked on the server thread **after** the
  destination is fully resolved and **immediately before** the Minecraft teleport call. It returns
  `Allow` or `Deny(reason)` (a controlled `TeleportVetoReason`); it can never rewrite the
  destination, suspend, or perform IO. It is applied by `TeleportPreflightGate`, which fails
  **closed** on a non-cancellation exception (the move is denied and the error logged) and preserves
  coroutine cancellation.
- `TeleportCommittedObserver` — a read-only callback invoked **after** a confirmed commit and the
  cooldown record. It receives an immutable `CommittedTeleport` (subject, `TeleportOrigin`, from/to
  endpoints) and cannot change the result. Its exceptions are logged and isolated; other observers
  and the teleport are unaffected.

The public hook types use `TeleportOrigin` and `TeleportEndpoint` (strings and primitives) rather
than the internal `TeleportCause`/`StoredPosition`, so no mutable Minecraft object is exposed and no
internal type leaks across the module boundary. Internal causes map onto the narrower origin set
(`JAIL_CONFINEMENT`/`JAIL_CORRECTION`/`JAIL_RELEASE` → `JAIL`; `TOP`/`BOTTOM`/`JUMP` → `TERRAIN`).

Both default to no-ops (`TeleportPreflight.AllowAll`, `TeleportCommittedObserver.None`), so a server
without a compat adapter behaves exactly as before.

## Consequences

- No `TeleportPipeline<List<Stage>>`, no reorderable stages, no mutable listener list, no global
  registry. The composition root injects at most one application-level policy through
  `createMovementFeature`.
- The veto is honored before `player.teleport`; a denial records no history and consumes no
  cooldown.
- An observer failure after a committed move never reverses the commit (this is the P1-B/TP-006
  rule).
- A future concrete land-claim integration is a `platform/<loader>` compat adapter bound at startup;
  `movement` never imports the target mod.
