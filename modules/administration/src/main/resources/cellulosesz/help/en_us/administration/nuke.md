# /nuke

Create a large explosion.

## Behavior

Drops a 5x5 grid of primed TNT 64 blocks above the highest block at each target column. The TNT is
tagged so its explosions are cosmetic: they produce the effect but destroy no blocks and deal no
damage. Without a target, every online player is nuked; otherwise each named player is.

## Parameters

- `players`: optional space-separated player names; omit to target every online player.

## Example

- `/nuke`
- `/nuke Alice`

## Failures

- None of the named players are online.

## Related commands

- `/fireball`, `/firework`, `/lightning`
