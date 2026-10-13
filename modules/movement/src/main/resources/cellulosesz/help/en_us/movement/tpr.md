# /tpr

Teleport to a safe random location inside a configured region.

## Behavior

Without an argument, the search uses the region named `default` in your current dimension; if none
is configured, the command fails. With a region name, the search uses that region and fails when it
is unknown, disabled, or set in another dimension.

Candidates are drawn uniformly by area from the region's annulus, only a location safe to stand on
is accepted, and the server caps how hard it searches and how often you can use it. If the region
does not overlap the world border at all, the search stops immediately.

## Limits and cautions

- Default search: up to 16 attempts within 20 seconds.
- Default cooldown: 120 seconds.
- Chunk generation is off by default, so the search skips unloaded areas.
- A search is cancelled if the region's center or radii change before it finishes.
- Ask an admin to add a region with `/settpr`.

## Examples

- `/tpr`
- `/tpr wild`

## Failures

- Random teleport is disabled.
- No default region is configured for your dimension.
- The named region is unknown, disabled, or in another dimension.
- The region has no area inside the world border.
- No safe location was found in time.
- The region changed during the search.
- You are still on cooldown.

## Related

- `/settpr`
