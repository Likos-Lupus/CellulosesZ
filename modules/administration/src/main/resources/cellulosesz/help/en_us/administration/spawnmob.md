# /spawnmob

Spawn one or more mobs, optionally mounted.

## Behavior

Spawns the requested entity type at your position, or at the target's position. The amount is capped
by `[administration.entityTools].maxSpawnAmount`; when the request is reduced the reply reports both
the requested and the spawned count.

`entity` accepts `mob[:data][,mount[:data]]`. When a mount is present, every spawned mob rides its
own mount and the call needs `cellulosesz.spawnmob.stack`. Entity data after `:` is accepted but
ignored in this build.

## Parameters

- `entity`: entity type id, optionally `:data` and/or `,mount`; for example `minecraft:cow`,
  `zombie,chicken`.
- `amount`: optional number to spawn; defaults to 1.
- `player`: optional target whose position is used; defaults to yourself.

## Example

- `/spawnmob cow`
- `/spawnmob zombie 10`
- `/spawnmob cow 5 Alice`
- `/spawnmob skeleton,spider`

## Failures

- The entity or mount type is unknown.
- The target is not online.
- A mount is requested without `cellulosesz.spawnmob.stack`.

## Related commands

- `/spawner`, `/remove`
