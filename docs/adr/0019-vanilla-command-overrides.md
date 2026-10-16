# 0019 — Vanilla command overrides and protected `cz` fallbacks

- **Status**: Accepted
- **Date**: 2026-10-16
- **Related**: `core/command/registry/VanillaCommandCatalog`,
  `core/command/registry/CommandRoutePlan`, ADR 0014, ADR 0015

## Context

Several CellulosesZ commands share a literal with a vanilla command (`/me`, `/tp`, `/kill`,
`/gamemode`). Registering the same root twice lets Brigadier merge the nodes by name, which silently
mixes argument types and executors — the exact problem ADR 0014 began to remove.

## Decision

1. **One audited catalog.** `VanillaCommandCatalog` is the single list of vanilla-conflicting
   commands. Each entry has a `TakeoverSupport`:
    - `VERIFIED` (`me`) — may be configured for takeover.
    - `CANDIDATE` (`tp`) — catalogued and given a fallback, but not configurable yet (see the
      `/tp` spike).
    - `NOT_SUPPORTED` (`kill`, `gamemode`) — fallback only.
2. **Protected single-segment fallbacks.** Every catalogued command gets `cz` + literal (`czme`,
   `cztp`, `czkill`, `czgamemode`) as a first-class `CZ_FALLBACK` route. It is **not** a DSL alias
   and is **not** gated by `commands.shortNamesEnabled` / `commands.aliasesEnabled`. There is **no
   `/cz` root command**.
3. **Explicit, single switch.** `[commands.vanillaOverrides]` (`enabled`, default `false`;
   `commands` allowlist) is the only place takeover is decided. When disabled, vanilla keeps the
   literal. Only `VERIFIED` entries may be listed; unknown/unverified/non-supported names fail
   startup validation with a precise message. A change is a startup binding (ADR 0015).
4. **One authority for `/me`.** `/me` ownership is derived only from `effectiveVanillaRoute(ME, …)`;
   the removed `messaging.actionMessage.overrideVanillaMe` key is gone. The mixin cancels only the
   vanilla registration when the takeover is actually enabled.
5. **No silent merge.** The route planner never emits both a `VANILLA_OVERRIDE` and a `CANONICAL`
   for the same literal, and it fails on any duplicate literal inside a plan. `CommandRegistry`
   installs only into a free root and never overwrites a foreign node.
6. **Route-level permission.** A takeover may carry an extra permission
   (`cellulosesz.command.override.tp` for `tp`) so replacing a vanilla management entry never widens
   an ordinary player's access; `/cz<p>` keeps using the business node only.

## Compatibility warnings

- Short command names and aliases that collide with vanilla or another mod are skipped, not merged;
  the namespaced `/cellulosesz:<command>` entry is always registered. `/tp`, `/kill`, `/gamemode`
  now resolve to vanilla unless a verified takeover is enabled.
- The default behavior changed: a fresh install leaves `/me` to vanilla (the old default replaced
  it). This is recorded in `CHANGELOG.md`.

## Consequences

- Diagnostics (`/cellulosesz commands`) and empty routes distinguish
  `FALLBACK_COLLISION` / `TAKEOVER_NOT_CONFIRMED` from an ordinary short-name skip.
- Adding a new catalogued command is a deliberate, reviewed change; it is not inferred from name
  collisions.
