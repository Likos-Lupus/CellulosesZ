# /tpall

Teleport every online player to one destination.

## Behavior

- `/tpall` moves every other online player to you.
- `/tpall <player>` moves every other online player to `player`.

This is a staff override: no request is sent, and the destination's `/tptoggle` setting does not
apply.

## Parameters

- `player`: the destination player; defaults to you.

## Example

- `/tpall`
- `/tpall Notch`

## Limits and cautions

- Anyone who cannot be moved is skipped; the reply reports how many actually moved.
- Each move is checked against the per-world access gate when it is enabled.

## Failures

- The destination player is not online.
- You are not a player and no destination was given.

## Related

- `/tpo`, `/tpohere`, `/tpoffline`
