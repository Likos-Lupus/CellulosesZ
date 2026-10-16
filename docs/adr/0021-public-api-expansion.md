# ADR 0021 — Public API expansion: Services, Events and Extensions

- **Status**: Accepted
- **Date**: 2026-10-16
- **Supersedes/extends**: ADR 0016 (Public API v0), ADR 0018 (Compatibility hooks)

## Context

Public API v0 shipped a narrow, read-only contract (`players`, `visibility`, `teleports`) plus a
loader-native provider (Fabric `ObjectShare`, NeoForge `FML` extension point). Third parties could
observe, but could not *do* anything: no way to list/travel warps, claim/grant kits, send mail, run
moderation, or contribute a policy. This ADR records the expansion into a Mod-to-Mod **business
integration** surface, without turning CellulosesZ into a plugin runtime.

## Decision

### Shape: Services + Events + Extensions

`CellulosesZApi` keeps its original members and adds domain facades:
`player`, `movement`, `communication`, `kits`, `moderation`, `presentation`, `extensions`.

- **Services** do work through the existing domain services; they never re-implement business rules.
- **Events** are committed facts, immutable, server-thread, listener-isolated.
- **Extensions** (SPI) only *narrow* decisions: `TeleportPolicyChecker` (allow/deny before a
  commit), `KitEligibilityChecker` (extra deny), and help-document contribution.

### Call model (§4)

- Cached reads: `ApiQuery<T>` (`Available`/`NotReady`/`Stopping`) — immutable snapshots, no JDBC.
- Async work: `dispatcher.submit { ... }` returning `ApiRequestHandle`; completion is
  `ApiCallResult<T>` (`Completed`/`NotReady`/`Stopping`/`Unavailable`/`Failed(diagnosticId)`).
  Expected business refusals are the domain sealed results inside `Completed`; only infrastructure
  problems become `Failed`.
- `ApiRequestHandle.cancel()` (best-effort business cancel) is distinct from `Subscription.close()`
  (unsubscribe); a request past its commit point returns `false` from `cancel()`.

### Threading (§4.4)

Domain result callbacks and events are delivered on the Minecraft server thread while a session is
active. `ApiRequestDispatcher` owns a kernel coroutine, never a raw executor, and never blocks.

### Authorization (§5)

- Player actions go through `PermissionService` (loader bridge; LuckPerms is a provider).
- System actions require a server-configured capability under `[api]` (`enabled=false` by default,
  `[api.addons."<id>"] allowedCapabilities=[...]`); `AddonAccess.isAllowed` re-reads the live
  snapshot.
- `AddonDescriptor.id` is attribution only — same-JVM mods are **not** a security sandbox.

### Provider lifecycle (A02)

`ApiIntegrationProvider` gained `whenRegistrationOpen` (startup contribution window) and now queues
`whenReady` when no session exists yet (was silently dropped). A stopped session's views never
revive; `markStopped` releases the session/api references.

### Extensions composition (§12)

`ApplicationExtensionHub` owns the startup window and ordered, immutable contributions.
`application` composes every `TeleportPolicyChecker` into the single `TeleportPreflight` the
movement feature receives (`ComposedTeleportPreflight`, fail-closed on a policy exception) —
movement stays a single-injection feature, it never becomes a listener registry.

## Consequences

- Feature modules gained narrow **integration ports** (`MovementIntegrationPort`,
  `PlayerIntegrationPort`, `CommunicationIntegrationPort`, `KitIntegrationPort`,
  `ModerationIntegrationPort`) exposed from their facades; only `application` maps them to the
  public contract, so no feature depends on `:modules:api`.
- A new durable, idempotent kit-reward path (`cz_kit_grants`, `KitGrantService`) was added — a
  RESERVED/DELIVERED ledger; a re-run of the same `grantId` never double-delivers.
- `:modules:api` remains pure JVM; the ABI baseline (`modules/api/api/api.api`) is regenerated via
  the reviewed `updateKotlinAbi` action.
- `AddonPresentationApi.sendNotice` renders addon text through a runtime
  `core.text.i18n.MessageOverlay` that collects addon-registered translations (see Closed gaps).

## Known gaps (tracked, not silently passed)

- **A18–A20**: no compiled sample addon projects / live-server matrix yet; verification so far is
  L0 + L1-structural (`compatSmokeBothLoaders`).
- Moderation `isMuted` is a coarse ungated read (mirrors `VisibilityLookup.canSee`).
- System-mail `title`/`deduplicationKey` are accepted by the integration port but not yet persisted:
  `MailMessage`/`cz_mail_messages` lack the columns, and the storage dialect has no `ALTER TABLE`
  facility, so persisting them needs a schema-migration story (ADR-worthy) before it is safe for
  existing installs.

## Closed gaps

- **A13**: `KitEligibilityChecker` is now composed — `ComposedKitEligibility` (fail-closed) is
  passed to `KitService` via `createUtilityFeature(kitEligibility = …)` and evaluated before the
  inventory precheck; a denial returns `KitClaimResult.Denied` →
  `KitClaimOutcome.Denied(reasonKey)`.
- **Addon translation catalog**: `AddonRegistration.registerTranslations` plus the application
  `AddonMessageOverlay` (namespaced `<addonId>.<key>`, malformed templates rejected) back the
  `sendNotice` resolver; `LocalizedMessages` consults the overlay before the bundled catalog.
- **Event coverage**: in-game private messages now feed the communication event sink (API- and
  command-originated paths both reported, no double-emit).

## Alternatives considered

- Bukkit/Paper-style plugin loader or `ServiceLoader`: rejected (ADR 0002/0016 — no dynamic module
  discovery, single composition root).
- Exposing repositories/services directly: rejected (leaks JDBC, Hikari, Minecraft internals).
- Making events cancellable/veto-capable: rejected — pre-commit decisions are the SPI; committed
  facts are observation only.
