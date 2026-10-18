# Mixin compatibility audit (Minecraft 26.1.2)

CellulosesZ ships five mixins. Each is reviewed here for its injection point, default behaviour,
conflict surface with other mods, failure mode and the decision for this stage. "Fail closed" means
a broken injection must not silently remove a safety constraint; "fail open" means an optional
cosmetic feature may simply do nothing.

All five live in a `mixin` package (Java only) because Mixin's Kotlin support is incomplete. All
mixin JSON files keep `required: true` and `defaultRequire: 1`.

| Mixin                         | Target                                                       | Injection           | Default                                | Decision                                                                                                             | Failure mode                                                                                                                                                 |
|-------------------------------|--------------------------------------------------------------|---------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CommandsRestrictionMixin`    | `Commands.performCommand`, `Commands.performPrefixedCommand` | HEAD, cancellable   | `restrictCommands=false`               | Keep. Rule moved to the Kotlin `JailRestrictionSnapshot`; the mixin only reads it.                                   | Fail closed: a broken target must fail the build/start, never let a confined player run blocked commands.                                                    |
| `PlayerListRespawnMixin`      | `PlayerList.respawn`                                         | RETURN              | marks for immediate re-check           | Keep. No equivalent platform event was verified for this version; the RETURN capture only reads the returned player. | Fail open is unacceptable; monitor only.                                                                                                                     |
| `ServerPlayerVisibilityMixin` | `ServerPlayer.broadcastToPlayer`                             | HEAD, cancellable   | vanish target hidden                   | Keep. Strictly one-directional: only ever returns `false`, never `true`.                                             | Fail closed for hiding; it never un-hides.                                                                                                                   |
| `PlayerDisplayNameMixin`      | `Player.getDisplayName`                                      | RETURN, cancellable | `applyToVanillaDisplay=false`          | Keep. Opt-in only; `getName()` is untouched so command argument resolution is unaffected.                            | Fail open: an optional cosmetic override may do nothing.                                                                                                     |
| `EmoteCommandsMixin`          | `EmoteCommands.register`                                     | HEAD, cancellable   | `commands.vanillaOverrides` lists `me` | Keep, driven by the effective vanilla route (`MeOverrideState`). Cancels only the vanilla registration.              | Fail open for the vanilla command: if the takeover is enabled, the DSL `/me` still registers; if the mixin ever failed, vanilla `/me` would simply reappear. |

## Ordering and coexistence notes

- `CommandsRestrictionMixin` hooks both dispatcher entry points so a nested `performPrefixedCommand`
  call is covered; the check is idempotent, so a player is never double-notified.
- `ServerPlayerVisibilityMixin` must never invert another mod's `false`; it can only further
  restrict.
- `PlayerDisplayNameMixin` uses `@At("RETURN")`, which observes a possibly already-modified
  `Component`. It is opt-in and defaults off precisely because it cannot reliably tell whether
  another mod has already changed the display name.
- `EmoteCommandsMixin` does not touch third-party `/me` nodes: it cancels vanilla's own registration
  at HEAD, and CellulosesZ then performs its own occupancy check (see ADR 0014).

## Deferred

Moving `PlayerListRespawnMixin` to a platform respawn event requires an event that fires after the
player object is fully available and covers death, dimension rebuild and the end portal. That is not
confirmed for 26.1.2 and is deferred; the current mixin is retained with an integration test.
