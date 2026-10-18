# /tpo

Teleport yourself to a player, or one player to another, without a request.

## Behavior

- `/tpo <player>` teleports you to `player`.
- `/tpo <player> <other>` teleports `other` to `player`.

This is a staff override, so the destination's `/tptoggle` setting does not apply.

## Parameters

- `player`: the destination player.
- `other`: the player to move when two names are given; defaults to you.

## Example

- `/tpo Notch`
- `/tpo Notch Dinnerbone`

## Failures

- A named player is not online.
- The destination is unsafe or outside the world border.
- Per-world access is enabled and the moved player is not allowed into that world.

## Related

- `/tpohere`, `/tpoffline`, `/tpall`, `/tp`
