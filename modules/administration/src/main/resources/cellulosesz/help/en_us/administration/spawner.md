# /spawner

Change the block entity behind the spawner you are looking at.

## Behavior

Retargets the spawner you are looking at to a new entity type. An optional delay in ticks overrides
how long the spawner waits between spawns, and requires the delay permission.

## Parameters

- `entity`: the entity type id, for example `minecraft:zombie` or `zombie`.
- `delay`: optional spawn delay in ticks, 1 up to 72000.

## Example

- `/spawner zombie`
- `/spawner minecraft:skeleton 200`

## Failures

- The entity type is unknown.
- You are not looking at a spawner block.

## Related commands

- `/spawnmob`, `/remove`
