# ADR 0005 — Communication bounded context

## Context

The communication module began as a command-first `/msg` + `/reply` pair backed by an in-memory
last-conversation map. It had no settings owner, no persistence, no per-player privacy controls, no
durable messaging, and no typed cross-feature integration. Two defects were known: the application
wired `canSend = { administration.isMuted(it) }` while the command treated `false` as "blocked"
(inverting mute enforcement), and quitting a player removed only their own reply key, leaving
dangling references.

EssentialsX is treated as **feature prior art only**. Its command set and privacy/durability needs
inform the problem space, but none of its API, configuration, permission, data, or command semantics
are a compatibility target.

## Decision

Communication is a bounded context composed of small owners, not a `CommunicationManager`:

- `PrivateMessageService` + `MinecraftMessagingBackend` — the private-message delivery transaction.
- `ReplyState` — session-only reply memory with `ReplyMode` and monotonic timeout.
- `MessagingPreferencesService` + `FileMessagingPreferencesRepository` — durable `/ignore` and
  `/msgtoggle`.
- `MailService` + `FileMailboxRepository` + `MailRateLimiter` — durable, offline-capable mail.
- `HelpOpService` — staff support channel.
- `AnnouncementService` — moderator broadcasts.
- `CommunicationIntegration` — the three typed capabilities the composition root injects
  (`PrivateMessageSenderGate`, `PrivateMessageReachability`, `PrivateMessageObserver`).

The composition root owns all cross-feature wiring; communication never imports administration.

## Invariants

- **C1 Delivery before state.** Reply state and delivered observations update only after the target
  message is successfully delivered.
- **C2 Durable preferences publish after persistence.** `/ignore` and `/msgtoggle` changes reach
  runtime state only after the atomic file write succeeds (write-before-publish).
- **C3 Direct messaging is session-only.** `/msg`/`/reply` content is never persisted; only explicit
  Mail is durable.
- **C4 Feature isolation.** Communication never imports administration; mute, vanish and social spy
  are composed through typed capabilities in `application`.
- **C5 Privacy is not a presence oracle.** Ignore state, receive-toggle state and vanish are not
  disclosed through overly specific sender-facing errors (vanished/unreachable targets read as "not
  online"; ignored mail senders see a normal success).
- **C6 Transient timing is monotonic.** Reply timeout and rate limiting use a monotonic
  `TimeSource`.
- **C7 Durable expiry is wall-clock.** Temporary mail uses `Clock`/`Instant` so expiry survives
  restart.
- **C8 No universal user profile.** Preferences and Mail are independently owned state, never fields
  in a global `UserData` object.
- **C9 Literal user content.** User-controlled text is rendered as a literal component; no
  formatting language is interpreted.
- **C10 EssentialsX is prior art only.** Commands, config, permissions and data formats are not
  compatibility contracts.
- **C11 Offline identity is network-free.** Offline name resolution uses only identities this server
  already knows; Minecraft's name-based profile cache performs a Mojang lookup on a miss and must
  never be used on a command path. (From Minecraft 26.1.2, `ProfileResolver.Cached` /
  `CachedUserNameToIdResolver.get(String)` reach the network; only `get(UUID)` is purely local.)

## Consequences

- A `ServerThreadRunner` seam lets preference and mail services publish on the server thread while
  remaining unit testable without a running server.
- `MinecraftKnownPlayerResolver` lives in `minecraft-core` and is shared by administration and
  communication, avoiding two identity indexes.
- The pure duration parser (`DurationParser`/`TemporaryDuration`) moved from administration to
  `foundation`, because temporary punishments and temporary mail share the same grammar.
- `verifyArchitecture` enforces that `StorageJson`/`AtomicFile` in communication appear only in its
  two repositories, in addition to the existing command-file IO ban.
- Mail is stored per owner under `communication/mail/<uuid>.json`; preferences under
  `communication/preferences/<uuid>.json`. Corrupt files raise a typed `*DataException` and are
  never silently overwritten.
