# /remove

Remove entities in bulk.

## Behavior

Removes matching entities and reports how many were removed. The named selectors are `all`, `tamed`,
`named`, `drops`, `arrows`, `boats`, `minecarts`, `xp`, `paintings`, `itemframes`, `endercrystals`,
`monsters`, `animals`, `ambient` and `mobs`; any other word is read as an entity type id. Players
are never removed. The optional last argument is either a radius in blocks around you or the word
`world`; with nothing there the whole world is searched.

## Parameters

- `selector`: a named selector or an entity type id.
- `scope`: optional radius in blocks, or `world`.

## Example

- `/remove drops`
- `/remove monsters 32`
- `/remove minecraft:arrow world`

## Failures

- The selector is unrecognized.
- The radius is missing, zero, negative, or above the configured cap.

## Related commands

- `/spawnmob`, `/spawner`
