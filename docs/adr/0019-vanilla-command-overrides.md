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
3. **Takeover is the default, and reversible.** `[commands.vanillaOverrides]` (`enabled`, default
   `true`; `commands` allowlist — empty means every catalogued command) is the only place takeover
   is decided. When disabled, vanilla keeps every literal and only the `cz`/namespaced routes
   remain. Only `VERIFIED` entries are eligible; unknown/unverified/non-supported names fail startup
   validation with a precise message. A change is a startup binding (ADR 0015).
4. **The displaced command stays reachable at `/minecraft:<literal>`.** Before the vanilla root
   child is replaced, `CommandRegistry` captures it, detaches it and installs a
   `minecraft:<literal>` literal that redirects to it with the same requirement copied. Brigadier
   has no removal API, so `ReflectiveCommandTreeMutator` (the only reflective code in the command
   path) removes the node from `children`/`literals`/`arguments`. A Mixin was evaluated first and
   rejected: Brigadier is a loader-provided library loaded before Mixin transforms anything, so the
   added method never applies (verified in a dev run — the takeover fell back to keeping vanilla).
   The command-tree packet serialises redirect targets, so the client still receives the vanilla
   subtree (arguments and completion included). **Invariant:** the takeover only happens after the
   alias is confirmed; if the alias cannot be installed, vanilla is restored and the route reports
   `TAKEOVER_NOT_CONFIRMED`.
5. **One authority for `/me`.** `/me` ownership is derived only from `effectiveVanillaRoute(ME, …)`;
   the removed `messaging.actionMessage.overrideVanillaMe` key is gone. The old mixin that cancelled
   vanilla `/me` registration is gone with it: cancelling would leave nothing for the alias to point
   at.
6. **No silent merge.** The route planner never emits both a `VANILLA_OVERRIDE` and a `CANONICAL`
   for the same literal, and it fails on any duplicate literal inside a plan. `CommandRegistry`
   never merges into a foreign node: a conflicting literal is either taken over (vanilla only) or
   skipped.
6. **Route-level permission.** A takeover may carry an extra permission
   (`cellulosesz.command.override.tp` for `tp`) so replacing a vanilla management entry never widens
   an ordinary player's access; `/cz<p>` keeps using the business node only.

## Compatibility warnings

- Short command names and aliases that collide with another mod are skipped, not merged; the
  namespaced `/cellulosesz:<command>` entry is always registered. A vanilla collision is replaced
  when the takeover policy allows it, and `/minecraft:<literal>` keeps the vanilla command.
- `/help` lists the `minecraft:*` aliases, and `/execute run <literal>` resolves to CellulosesZ
  after a takeover (use `execute run minecraft:<literal>` for vanilla).
- A provider that does not answer the Fabric permission request falls back to the vanilla level; see
  ADR 0008.

## Consequences

- Diagnostics (`/cellulosesz commands`) and empty routes distinguish
  `FALLBACK_COLLISION` / `TAKEOVER_NOT_CONFIRMED` from an ordinary short-name skip.
- Adding a new catalogued command is a deliberate, reviewed change; it is not inferred from name
  collisions.
