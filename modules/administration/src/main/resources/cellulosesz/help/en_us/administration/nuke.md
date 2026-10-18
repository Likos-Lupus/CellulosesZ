# /nuke

Create a large explosion.

## Behavior

Detonates one large explosion at the target's position, or at your own position without a target.
The radius is capped by `[administration.worldTools].maxNukeRadius`. This destroys terrain; use it
with care.

## Parameters

- `player`: optional target; defaults to yourself.

## Example

- `/nuke`
- `/nuke Alice`

## Failures

- The target is not online.

## Related commands

- `/fireball`, `/firework`, `/lightning`
