# ADR 0004 — Administration and moderation foundation

## Context

The administration module originally held a handful of command-first utilities (`/heal`, `/feed`,
`/kick`, `/fly`, `/god`) that mutated `ServerPlayer` state directly. There were no settings, no
persistence, no audit, no typed failures, and no enforcement outside the command bodies. Mature
servers, however, need account/IP bans, temporary punishments that survive restarts, mutes that
block chat and private messages, target protection, and an audit trail.

EssentialsX is treated as **feature prior art only**: its command set and server-operator needs
inform the problem space, but none of its API, configuration, permission, data, or command semantics
are a compatibility target.

## Decision

Administration is a bounded context composed of small owners, not a `ModerationManager`:

- `KickService` — online-only kicks.
- `BanService` + `MinecraftBanBackend` — account/IP bans backed by Minecraft's **native** ban lists.
- `MuteService` + `FileMuteRepository` — durable mutes owned by CellulosesZ.
- `ModerationAuditService` — one immutable audit record per action.
- `TargetProtectionPolicy` — the current capability-based safety floor.
- `PlayerControlService` — kill, game mode, and console-only sudo.
- `SocialSpyService` — session-only private-message observation.
- `VanishService` + `ServerPlayerVisibilityMixin` — true tracking-level vanish. The mixin is Java,
  the module's only non-Kotlin source, permitted by the `mixin`-package Java exception.

Expected failures are sealed values; exceptions are reserved for storage/IO/invariant faults.
Composition is explicit in `createAdministrationFeature`; the public facade stays small.

## Invariants

- **A1 Native bans are authoritative.** Account and IP bans use Minecraft's native ban lists as the
  sole enforcement source of truth. There is no second `bans.json`.
- **A2 Durable restrictions publish after persistence.** A mute becomes active only after its file
  is written; it is removed only after its file is deleted.
- **A3 Server-thread ownership.** Minecraft state and active moderation state are server-thread
  owned; repository IO runs on `Dispatchers.IO`.
- **A4 Wall-clock expiry.** Restart-surviving punishments use `Clock`/`Instant`, never monotonic
  gameplay time.
- **A5 Expected failures are values.** Unknown/protected/self target, already muted or banned, and
  duration too long are sealed outcomes.
- **A6 No feature-to-feature moderation dependency.** Mute/social-spy wiring is composed in the
  application through minimal capabilities (`canSend`, `observePrivateMessage`).
- **A7 Audit does not roll back enforcement.** An audit write failure is logged and surfaced but
  never pretends an applied punishment was reverted.
- **A8 No fake vanish.** Vanish suppresses entity tracking and the tab list; invisibility effects
  are not a substitute.
- **A9 No implicit IP history.** CellulosesZ does not persist last-known IPs merely to emulate
  convenience commands; `/banip` takes a literal or an online player. IPv4 and IPv6 literals are
  accepted and canonicalized with Guava `InetAddresses` (no DNS). Because vanilla `IpBanList`
  cannot extract an IPv6 address on the login path, an IPv6 IP ban disconnects currently-online
  players but does not block a reconnect; account bans are unaffected.
- **A10 Command execution is single-path.** Only `MinecraftBanBackend` touches native ban lists and
  only `MinecraftPlayerControlBackend` kills, changes game mode, or runs a command as a player.

## Alternatives

- A universal `PunishmentManager`/`UserData` — rejected: bans and mutes have different sources of
  truth and lifecycles.
- A CellulosesZ-owned ban list — rejected: vanilla already enforces, persists, and interoperates.
- Toggle-style `/mute` — rejected: explicit `/mute`/`/tempmute`/`/unmute` is predictable and
  idempotent.
- Invisibility-potion vanish — rejected as not real vanish.

## Consequences

- Bans interoperate with vanilla `/ban` and `/pardon` and survive restarts for free.
- New moderation actions become thin: resolve, protect, commit, notify, audit.
- `verifyArchitecture` fails on native ban access or operator execution outside the backends, and on
  file IO in command files.
- Vanish requires the tracking mixin; the Java mixin is remapped by the Architectury transformer for
  each loader. `verifyArchitecture` allows Java only in a `mixin` package (plus the loader shims)
  because Mixin's Kotlin support is incomplete.

## Validation

- Unit tests: `DurationParserTest`, `ModerationReasonTest`, `MuteFileTest`,
  `FileMuteRepositoryTest`, `FileModerationAuditRepositoryTest`.
- Architecture: `verifyArchitecture` enforces single-path ban/control access and command IO bans.
- Build: `checkModules`, `chiseledBuild`, `inspectArtifacts`.
