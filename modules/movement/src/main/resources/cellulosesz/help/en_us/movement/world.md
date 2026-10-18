# /world

Show the world you are in, list the loaded worlds, or teleport to a world's spawn.

## Behavior

- `/world` shows your current world and its dimension id.
- `/world list` lists every loaded world with its dimension id.
- `/world <name>` teleports you to that world's spawn.

A `name` may be a configured alias or a full dimension id such as `minecraft:the_end`. Aliases come
from `[movement.worlds.aliases]` (`overworld`, `nether` and `end` by default).

## Parameters

- `name`: a world alias or a dimension id.

## Example

- `/world`
- `/world nether`
- `/world minecraft:the_end`
- `/world list`

## Failures

- No loaded world matches the name.
- The destination is unsafe or outside the world border.
- Per-world access is enabled and the world is not allowed.

## Related

- `/spawn`, `/home`, `/warp`
