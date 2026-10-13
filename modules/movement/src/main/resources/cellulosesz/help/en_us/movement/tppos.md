# /tppos

Teleport yourself to coordinates.

## Behavior

Requires moderator permission. The destination skips the safety search, so pick a safe spot.

## Parameters

- `x`, `y`, `z`: destination coordinates.
- `dimension`: optional dimension id; defaults to your current dimension.

## Example

- `/tppos 100 64 -200`
- `/tppos 0 70 0 minecraft:overworld`

## Failures

- The dimension id is invalid or not loaded.
- The destination is outside the world border.

## Related

- `/tp`, `/tphere`
