# /tpoffline

Teleport to the last known location of an offline player.

## Behavior

Uses the position recorded when the player left the server. The last known location is written on
quit; when none was stored, the command reports that and moves nobody.

## Parameters

- `player`: the player whose last known location is the destination.

## Example

- `/tpoffline Notch`

## Failures

- The name is not a player this server knows.
- No last known location is stored for that player.
- The destination is unsafe or outside the world border.
- Per-world access is enabled and you are not allowed into that world.

## Related

- `/tpo`, `/tpohere`, `/tpall`
