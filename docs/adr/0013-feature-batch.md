# ADR 0013: Feature batch — player context, jail, RTP, tpauto, server pages, item editing

- Status: Accepted
- Date: 2026-10-13
- Relates to: ADR 0002 (modular monolith), ADR 0007 (storage), ADR 0012 (presentation)

## Context

A large feature batch added a new `player` bounded context and features across the existing
contexts. This ADR records the architectural decisions and boundaries that the batch introduced.

## Decision

1. **New `modules/player` bounded context.** Owns sessions, presence, AFK, nearby and nicknames. It
   depends only on `foundation` + `minecraft-core`; only `application` wires it. A
   `CommandCategory.PLAYER` and `player` translation/help category were added.
2. **New tables (no ALTER on existing tables).** Every table is owned by a `SqlSchemaContributor`:
   `cz_player_sessions`, `cz_player_nicknames` (player); `cz_random_teleport_regions`,
   `cz_warp_metadata`, `cz_tpauto_preferences`, `cz_tpauto_allowed` (movement);
   `cz_jail_locations`, `cz_jail_sentences` (administration).
3. **Cold-migration compatibility.** `StorageMigrator` tolerates a *wholly absent* table on the
   source side (verified via `DatabaseMetaData.getTables`, case-insensitive; all other errors stay
   fatal), so an older database can be migrated forward into the new schema.
4. **Cross-context ports live at the edges.** Administration defines `JailTransferPort` and
   `ConfinementQuery` without importing movement; movement defines a local
   `TeleportConfinementCheck`; `application` adapts them (e.g.
   `modules/application/.../application/port/MovementJailTransferPort.kt`). No feature→feature
   compile dependency.
5. **Single teleport path preserved.** Every move (jail transfer, RTP, terrain, tpaall) goes through
   `TeleportCoordinator`; the confinement gate is consulted inside it (a confined subject is denied
   every player-instigated cause; `ADMIN`/`JAIL` bypass).
6. **`/tpauto` is conservative.** Auto-accept only applies to `TO_TARGET` requests (the requester
   comes to the owner); `BRING_TARGET` is never auto-accepted. Default is OFF.
7. **Server pages are files, not storage.** `/motd`/`/rules`/`/info`/`/customtext` read Markdown
   from
   `<config>/cellulosesz/pages/<lang>/`, rendered whole (ADR 0012: no pagination), reloaded on
   demand.
8. **`/me` ownership.** Vanilla `/me` is not registered through the DSL (its argument is a
   `Component`); a Brigadier executor overlay (`VanillaMeOverlay`) reuses the vanilla argument type
   when `overrideVanillaMe=true`, with `/czme` as the guaranteed fallback.
9. **Item editing stays behind a backend.** `/itemname`/`/itemlore`/`/hat` mutate items only inside
   `MinecraftItemUtilityBackend` (DataComponents API); clear-inventory uses a single-use
   confirmation code.
10. **Adjustments are bounded and session-scoped.** `/exp` clamps to `maximumExperienceAdjustment`;
    `/speed` snapshots the base attribute value and restores it on quit/stop (never persisted).

## Consequences

- `README.md` command catalog is regenerated; help docs are hand-written and coverage-checked
  (`verifyHelpCopy`). All new commands ship `en_us` + `zh_cn` help and translations with matching
  key and placeholder sets.
- The batch does not change any existing command's syntax, permission meaning or persistence.
