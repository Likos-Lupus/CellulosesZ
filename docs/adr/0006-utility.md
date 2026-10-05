# ADR 0006 — Utility bounded context

## Context

The utility module began as a single `kits.json` document plus a handful of commands that mutated
`ItemStack`s inline. It had no settings owner, no reuse policy, no durable claim history, no
inventory-capacity semantics, and persisted `kits.json` at the world root instead of a utility-owned
namespace. Kit delivery returned `Success` even when items were dropped or the inventory was full,
and `/createkit` silently overwrote existing kits.

EssentialsX is treated as **feature prior art only**. Its kit and inventory needs inform the problem
space, but none of its commands, configuration, item syntax, permissions, data formats, or APIs are
a compatibility target.

## Decision

Utility owns *kits and item/inventory convenience*, not economy, moderation, teleportation,
communication, or world control. It is composed of small owners:

- `KitService` + `KitCatalogState` — server-thread kit catalog and the claim transaction.
- `FileKitRepository` — the durable v2 kit catalog at `utility/kits.json`.
- `KitInventoryBackend` / `MinecraftKitInventoryBackend` — the only live-inventory kit mutation
  path.
- `KitClaimRepository` / `FileKitClaimRepository` — per-player durable claim history at
  `utility/kit-claims/<uuid>.json`.
- `ItemUtilityService` + `ItemUtilityBackend` / `MinecraftItemUtilityBackend` — `/repair`, `/more`,
  `/condense`.
- `WorkstationService` + `WorkstationBackend` / `MinecraftWorkstationBackend` — portable vanilla
  workstation menus.
- `InspectionService` + `InspectionBackend` / `MinecraftInspectionBackend` — `/enderchest`,
  view-only `/invsee`, `/disposal`.

## Invariants

- **U1 Kit definitions are server-owned catalog state.** Definitions are loaded into a server-thread
  catalog and published only after durable repository writes.
- **U2 Claims are player-owned durable state.** Cooldown/one-time history lives in per-player claim
  files, separate from kit definitions.
- **U3 Claim reservation precedes delivery.** Cooldown/one-time kits persist a `RESERVED` record
  before any inventory mutation, so a crash fails closed instead of duplicating items.
- **U4 Delivery is explicit.** Overflow is either `REJECT` (all-or-nothing) or `DROP`; silent
  partial delivery is impossible.
- **U5 Persistent expiry uses wall-clock.** Cooldown history uses `Clock`/`Instant` (never monotonic
  time); a future timestamp still blocks.
- **U6 Minecraft state stays on the server thread.** Inventory mutation, item capture, drops and
  menu opening happen only on the server thread.
- **U7 Repository IO stays in repositories.** `StorageJson`/`AtomicFile` appear only in the two kit
  repositories.
- **U8 No universal user data.** Claims are not fields in a global player profile.
- **U9 Vanilla commands are not duplicated without value.** CellulosesZ does not reimplement
  `/give`, `/enchant`, or `/clear` for parity.
- **U10 EssentialsX is prior art only.** No EssentialsX compatibility is promised.

## Consequences

- Kit identity is a stable `KitId`: deleting and recreating a kit yields a new id, so old claims
  never leak onto the new definition, while `/updatekit` preserves the id and therefore the
  cooldown.
- Item components round-trip because `KitItemCodec` encodes/decodes `ItemStack.CODEC` with a
  registry-aware `RegistryOps` captured from the running server.
- Legacy `<world>/cellulosesz/kits.json` (v1) is migrated once into `utility/kits.json` (v2) and the
  legacy file is renamed to `kits.json.v1.bak`; a failed migration leaves the new file unwritten.
- Portable workstations default to disabled; `/more` and `/condense` default to disabled; `/repair`
  keeps its previous default-on behavior.
- Only vanilla menu types are used (`ChestMenu` + built-in workstation menus), so the mod remains
  client-loadable without any client screen code.
- `verifyArchitecture` enforces that utility `StorageJson`/`AtomicFile` usage is confined to the kit
  repositories and that live kit inventory mutation is confined to `MinecraftKitInventoryBackend`.
- Minecraft-dependent item encoding/decoding and menu behavior are covered by loader smoke tests;
  the unit suite covers kit naming, settings validation, claim persistence, and the claim
  transaction through fakes.
- `/showrecipe` is intentionally deferred: rendering every recipe type accurately requires
  version-sensitive `RecipeDisplay` handling, and an inaccurate recipe viewer is worse than none.
