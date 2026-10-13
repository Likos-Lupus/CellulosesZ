# /tp

Teleport yourself to a player, or move one player to another.

## Behavior

- `/tp <target>` teleports you to `target`.
- `/tp <from> <to>` moves `from` to `to` and needs the `tp` permission; servers usually reserve it
  for staff.

## Parameters

- `first`: the player to move when two names are given, otherwise your destination.
- `second`: optional destination player when moving someone else.

## Example

- `/tp Notch`
- `/tp Alice Bob`

## Failures

- A named player is not online.
- You are already at your own position.
- The destination is unsafe or outside the world border.

## Related

- `/tphere`, `/tppos`, `/tpa`
