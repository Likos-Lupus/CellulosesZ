# /spawnmob

Spawn one or more mobs.

## Behavior

Spawns the requested entity type at your position, or at the target's position. The amount is capped
by `[administration.entityTools].maxSpawnAmount`; when the request is reduced the reply reports both
the requested and the spawned count.

## Parameters

- `entity`: the entity type id, for example `minecraft:cow` or `cow`.
- `amount`: optional number to spawn; defaults to 1.
- `player`: optional target whose position is used; defaults to yourself.

## Example

- `/spawnmob cow`
- `/spawnmob zombie 10`
- `/spawnmob cow 5 Alice`

## Failures

- The entity type is unknown.
- The target is not online.

## Related commands

- `/spawner`, `/remove`
