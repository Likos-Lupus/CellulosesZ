# ADR 0017 — Mixin coexistence policy

- **Status**: Accepted
- **Date**: 2026-10-16
- **Related**: ADR 0014 (command route ownership), ADR 0004 (administration/moderation)

## Context

Five mixins inject into vanilla command, respawn, player-tracking and display-name code. They can
collide with other mods that hook the same methods. The previous jail command restriction used a
hardcoded `Set.of("helpop")` inside the Java mixin and a bare `restrictCommands` boolean flag, which
could not be validated, reloaded atomically, or tested without Minecraft.

## Decision

1. **Classify each mixin.** Safety-critical (`CommandsRestrictionMixin`,
   `ServerPlayerVisibilityMixin`)
   must fail closed; optional/display (`PlayerDisplayNameMixin`, `EmoteCommandsMixin`) may fail
   open. `required: true` / `defaultRequire: 1` stays for all five. Lowering a require level is
   never used to "make it start".
2. **Move logic out of Java.** `CommandsRestrictionMixin` now reads an immutable
   `JailRestrictionSnapshot(enabled, allowedRoutes)` and asks `isAllowed(firstToken)`. The Java side
   is a narrow bridge; the allow-list derivation lives in Kotlin
   (`JailCommandAccessPolicy.snapshotFrom`). Entries are matched on the normalized first token, and
   a plain entry also matches its `cellulosesz:<entry>` form. The default allow-list is still
   `helpop`.
3. **Atomic, reloadable snapshot.** `RestrictCommandsState` holds the snapshot in a `@Volatile`
   reference; `AdministrationFeature.applyJailCommandRestrictions()` replaces it wholesale on the
   server thread (startup and reload). Callers never see a partially updated rule.
4. **Validated config.** `administration.jail.allowedCommands` rejects blanks, spaces, wildcards and
   over-long lists, so a typo cannot accidentally open a command.
5. **No priority wars.** We do not raise mixin priorities to win against other mods. Interaction is
   expressed through snapshots and the single teleport/command paths.

## Consequences

- The jail restriction is now unit-testable as a pure decision and reloadable without a restart.
- `ServerPlayerVisibilityMixin` remains one-directional; it can only hide more, never reveal.
- Moving the respawn mixin to a platform event is explicitly deferred until such an event is
  verified for 26.1.2 (see `docs/analysis/mixin-compatibility-audit-26.1.2.md`).
