# /potion

Edit the held potion or apply its effects.

## Parameters

- `apply`: drink the held potion's effects yourself.
- `clear`: clear the held potion's effects.
- `effect:<id>[:seconds[:amplifier]]`: add an effect (default 30 seconds, amplifier 0).
- `player`: optional for `clear` and `effect:`; edit that player's held potion.

## Behavior

Edits the `POTION_CONTENTS` component of the held potion. Applying to yourself needs the apply
permission; editing another player needs the others permission. Moderator command.

## Examples

- `/potion effect:speed`
- `/potion effect:regeneration:10:1`
- `/potion clear`
- `/potion apply`

## Failures

- The held item is not a potion.
- The effect is unknown.

## Related commands

- `/item`
