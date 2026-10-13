# /settpr

Create and manage random teleport regions.

## Behavior

Requires moderator permission. A region is a named annulus (a ring) around a center point in a
single dimension; `/tpr` draws candidates uniformly by area between its minimum and maximum radius.
The center comes from where you run `create` or `center`. Every change to the center or the radii
increments the region revision, which cancels an in-flight search that is using the old shape.

- `/settpr list` lists every region and its dimension.
- `/settpr info <region>` shows one region's dimension, center, radius range and status.
- `/settpr create <region> <minRadius> <maxRadius>` creates or replaces a region centered on you.
- `/settpr center <region>` moves a region's center to your position, in the same dimension only.
- `/settpr range <region> <minRadius> <maxRadius>` changes a region's radii.
- `/settpr remove <region>` deletes a region.

## Parameters

- `region`: the region name. Lowercase letters, digits, `_` and `-`, up to 32 characters.
- `minRadius`: nearest distance from the center, in blocks. Must be `>= 0`.
- `maxRadius`: farthest distance from the center, in blocks. Must be greater than `minRadius` and
  within the server's random teleport limit.

## Examples

- `/settpr create wild 500 2000`
- `/settpr range wild 100 1500`
- `/settpr center wild`
- `/settpr info wild`
- `/settpr list`
- `/settpr remove wild`

## Failures

- The name is invalid.
- The radii are out of range.
- `center` targets a region in another dimension.
- The region limit has been reached.
- The region does not exist.
- Missing permission.

## Related

- `/tpr`
