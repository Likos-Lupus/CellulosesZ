# /tpohere

Teleport a player to you without a request.

## Behavior

Moves `player` to your current position immediately. This is a staff override, so the target's
`/tptoggle` setting does not apply.

## Parameters

- `player`: the player to move to you.

## Example

- `/tpohere Dinnerbone`

## Failures

- The named player is not online.
- The destination is unsafe or outside the world border.
- Per-world access is enabled and the moved player is not allowed into that world.

## Related

- `/tpo`, `/tpoffline`, `/tpall`, `/tphere`
