# /jump

Teleport to the block you are looking at.

## Behavior

Looks along your line of sight up to the configured distance and lands on the surface of the first
block it hits.

## Limits and cautions

- Default maximum distance: 32 blocks.
- If the targeted area isn't loaded you are not moved.

## Example

- `/jump`

## Failures

- The terrain feature is disabled.
- You aren't looking at a block within range.
- The area isn't loaded.
- No safe spot was found.

## Related

- `/top`, `/bottom`
