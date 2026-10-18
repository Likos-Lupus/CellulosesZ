# ADR 0023 — EssentialsX main-module parity

## Status

Accepted.

## Context

CellulosesZ is being brought to feature parity with the **main** EssentialsX module (not its Chat,
Protect, AntiBuild, Spawn, GeoIP, Discord, DiscordLink or XMPP modules). The requested scope
excludes the sign system, platform-only backend differences (Vault, NMS/Paper providers, bStats,
update checks, PlaceholderAPI internals) and any compatibility layer with EssentialsX. The mod is
unreleased, so compatibility shims and version bumps carry no value.

## Decision

- **No new module.** Lifecycle text goes to `communication`, world/teleport features to
  `movement`, worth/sell to `economy`, item/container tools to `utility`, operator tools and
  moderation details to `administration`, player queries to `player`, and cross-cutting command
  gating/backup to `application`.
- **One preference store.** Every new player toggle lives in the shared `cz_player_preferences`
  table behind `PlayerPreferenceService` (an in-memory read, one write path); features never read or
  write each other's storage.
- **Mixins where the platform has no equivalent.** Join/quit/death messages, the sleep vote, AFK
  movement/interaction and powertool clicks are implemented with mixins. They stay small, are
  registered per module, and are exercised by live-server scenarios; avoiding a mixin is not a
  reason to weaken behaviour.
- **Sale protocol.** `/sell` removes items only after a durable intent (with an exact stack
  snapshot) is written and marks the intent `TAKEN` immediately after removal; the ledger credit is
  idempotent by sale id. Recovery restores a `TAKEN` intent whose ledger row is absent. The design
  can lose items in the single write window but can never create money or duplicate items.
- **One command gate.** Costs, cooldowns, muted-command blocking and command social-spy are one
  ordered `CommandInterceptor` installed by the composition root. Brigadier permissions still run
  before it, and an interceptor may defer the body (a cost charge is a suspension) but calls
  `proceed` at most once on the server thread.
- **`/editsign` is in scope** as a text-only command; the sign system itself is not. Click and hover
  events are always stripped from edited text.
- **Help.** `/cellulosesz help [page]` paginates the command *list* only. Single-command
  documentation is shown whole; vanilla `/help` stays untouched unless explicitly overridden.
- **No compatibility work.** There is no version bump, deprecation shim, legacy config key or data
  migration. An existing development database may need to be reset.

## Consequences

- The parity surface is reachable through the existing facades; third-party addons get
  `player.preferences`, `movement.worlds`/last-known location, `TeleportTarget.World` and
  `economy.worth`, all deny-by-default where they mutate state.
- Mixins are part of the compatibility contract: a mapping change must be re-verified on 26.1.2 for
  both loaders.
- The sale state machine and command gate are deliberately explicit; future features should reuse
  them instead of adding another write path or another command wrapper.
