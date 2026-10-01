# ADR 0003 — Single teleport pipeline

## Context

Homes, warps, spawn, teleport requests, and direct teleport commands previously each reached the
Minecraft teleport API through a thin `movement.teleport.TeleportService`. That wrapper had no
concept of safety, delay, cancellation, cooldown, history, passenger handling, or late destination
resolution, and the result set (`Success`/`PlayerOffline`/`UnknownDimension`) could not express the
real failure space. Commands also ran their orchestration on `Dispatchers.IO`, which is the wrong
dispatcher for work that mostly touches server state.

Mature server teleportation is a subsystem, not a handful of `/tp` commands. Left unstructured, the
same concerns accrete into a god service or a middleware framework.

## Decision

All CellulosesZ-initiated player movement goes through one immutable intent and one coordinator:

```text
TeleportIntent → preflight → optional delay → late destination resolve
              → safety → commit → history → cooldown → TeleportOutcome
```

- `TeleportCoordinator` is the only orchestrator. Home, warp, spawn, request, `/back`, and direct
  commands only describe a `TeleportIntent` (subject, destination, cause, policy).
- `MinecraftTeleportBackend` is the only class that may call the Minecraft teleport API; enforced by
  `verifyArchitecture`.
- Expected failures are sealed `TeleportOutcome` values, never exceptions. Exceptions are reserved
  for storage/IO/invariant faults.
- `TeleportDestination.Player` is resolved at commit time, never snapshotted when the request is
  made.
- Transient state (`PendingTeleportService`, `TeleportCooldowns`, `TeleportRequestService`) is
  server-thread confined and in-memory; history (`/back`) is the only persisted teleport state.
- Policy is captured into the intent, so command handlers never assemble delay/cooldown/safety rules
  and there is no hidden bypass.

The following invariants are part of the contract:

- **T1 Single commit path** — only `MinecraftTeleportBackend` calls the teleport API.
- **T2 Server-thread ownership** — Minecraft world/entity state and teleport transient state belong
  to the server thread.
- **T3 IO isolation** — repositories switch to `Dispatchers.IO`; orchestration runs on the runtime's
  default dispatcher.
- **T4 Immutable intents.**
- **T5 Expected failures are values.**
- **T6 Dynamic destinations resolve late.**
- **T7 Safety by default** for ordinary player teleports.
- **T8 No hidden bypass** — unsafe/immediate behavior is an explicit policy (admin), not a special
  command that skips the pipeline.
- **T9 Version differences stay at the edge** — version-specific teleport code lives only in
  `MinecraftTeleportBackend`.
- **T10 EssentialsX is feature prior art only** — no API, config, data, command, or implementation
  compatibility.

## Alternatives

- Extend the old `TeleportService` in place — rejected: it has no seam for delay, safety, or
  history, and every caller would keep re-implementing error mapping.
- A middleware/stage pipeline (`TeleportPipeline`, `TeleportStage`, `TeleportContext`) — rejected:
  the transaction is small and fixed; explicit straight-line code is easier to reason about.
- Resolve player destinations eagerly when a request is accepted — rejected: a delayed teleport
  would deliver the subject to the target's stale position.
- Persist pending teleports/cooldowns — rejected: they are meaningless across a restart.

## Consequences

- New movement features become thin: build an intent, call the coordinator, map feedback.
- `TeleportOutcome` can grow without touching command files, because `TeleportFeedback` owns the
  generic error text.
- Safety scanning is bounded and deterministic, so it cannot become an unbounded world scan.
- The teleport backend is the natural (and only) home for future Minecraft-version divergence.
- `/back` history is a separate transaction level: a failed history write only costs `/back`, never
  rolls back a successful teleport.

## Rollback

If the pipeline proves too heavy, individual stages (delay, cooldown, history) can be disabled via
config or removed without changing command call sites, since commands only depend on
`TeleportIntent`/`TeleportOutcome`.

## Validation

- Unit tests: `SafeDestinationResolverTest`, `PendingTeleportServiceTest`, `TeleportCooldownsTest`,
  `FileTeleportHistoryRepositoryTest`, `TeleportCoordinatorTest`, `TeleportRequestServiceTest`,
  `StoredPositionTest`.
- Architecture: `verifyArchitecture` fails on any direct teleport call outside
  `MinecraftTeleportBackend`.
- Build: `checkModules`, `chiseledBuild`, `inspectArtifacts`.
