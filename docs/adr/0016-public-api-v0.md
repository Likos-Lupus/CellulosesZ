# ADR 0016 — Public API v0

- **Status**: Accepted (experimental; NeoForge discovery pending a spike)
- **Date**: 2026-10-16
- **Related**: ADR 0002 (modular monolith), ADR 0014 (command route ownership), ADR 0018
  (compatibility hooks)

## Context

CellulosesZ must let other mods coexist without depending on it (that is the compatibility work in
ADRs 0014/0017/0018). A small number of consumers additionally want to *integrate*: read a player's
identity, ask CellulosesZ's own visibility rule, or observe CellulosesZ's confirmed teleports. The
API must stay narrow, pure-JVM and independent of the mod runtime version.

## Decision

A new compile-time module `:modules:api` (`top.likoslupus.cellulosesz.api`) is a plain Kotlin/JVM
library with `explicitApi()`. It depends only on the Kotlin standard library — **no Minecraft, no
Loader, no database, no feature module, no Brigadier**.

**v0 surface (read-only + observation only):**

- Lifecycle: `CellulosesZApiProvider` (`availability()`, `whenReady`) and `ApiAvailability`.
- `CellulosesZApi` with `version: ApiVersion`, `players`, `visibility`, `teleports`.
- `PlayerLookup.findById(UUID): ApiQuery<PlayerIdentity?>` — `Available(null)` means unknown.
- `VisibilityLookup.canSee(viewerId, targetId): ApiQuery<Boolean>` — CellulosesZ's own rule only.
- `TeleportEvents.subscribe(TeleportListener)` — confirmed, CellulosesZ-initiated successes only;
  `TeleportCommitted.to` is the actual committed position.
- `ApiQuery<T>` (`Available`/`NotReady`/`Stopping`) for session gating.

**Explicitly not in v0:** any write/request API (a controlled `TeleportService.request` is deferred
until there is a trusted consumer and a permission/audit design), messaging/mute/economy, and
anything DB-, command-DSL- or kernel-related.

**Adapters** live in `application` (`application.api`): the concrete `CellulosesZApi` delegates to
the features (identity resolver, `AdministrationFeature.canBeSeenBy`, and the movement
`TeleportCommittedObserver`). The provider gates reads on readiness and fans out teleport events
with per-listener isolation.

**Distribution:** API classes are flattened into both loader distributions (one copy each);
consumers must depend on `cellulosesz-api` as `compileOnly` and must not bundle it.

**Discovery:** the composition root calls `PlatformServices.publishApi(provider)`. Fabric publishes
on the loader's `ObjectShare` under `cellulosesz:api-v0`. NeoForge discovery is **not yet verified**
for this target and is intentionally a warned no-op, so NeoForge is *contract-complete but not
release-ready*.

**ABI:** `kotlin { abiValidation() }` is enabled; the baseline `modules/api/api/api.api` is
committed and `checkKotlinAbi` runs as part of `:modules:api:check` (hence `checkModules`). CI never
runs `updateKotlinAbi`.

## Version policy

- `ApiVersion(major, minor)` is separate from the mod version; v0 is `major = 0` (experimental).
- `0.x` allows documented, motivation-carrying breaks, but a released artifact is never overwritten
  with different bytes; every break gets an ABI diff and a consumer migration note.
- Once `1.0.0` is tagged, SemVer applies to both ABI and semantics (threading/lifecycle/exception
  contracts count as breaking even when the JVM ABI tool does not flag them).

## Consequences

- `TeleportOriginKind` maps internal causes onto a stable set; unknown causes map to `OTHER` and
  never throw. `ApiQuery` is a sealed type; new *unavailability* reasons should prefer stable fields
  rather than unbounded new subtypes.
- Consumers that need CellulosesZ at runtime declare a Loader mod dependency; the API is an
  interoperability enhancement, never a coexistence prerequisite.
