# /pweather

Set a personal weather.

## Behavior

Shows or sets the weather only as you see it, without changing the world. Accepts `list`, `reset`,
`clear`, `rain` and `thunder`. The override is re-sent whenever you join or change world, and
cleared when you quit.

## Parameters

- `value`: `clear`, `rain` or `thunder`.
- `list`: show your current personal weather.
- `reset`: return to the world's weather.
- `player`: optional target for the `others` permission; defaults to yourself.

## Example

- `/pweather clear`
- `/pweather rain`
- `/pweather reset`

## Failures

- The weather value is unrecognized.

## Related commands

- `/ptime`, `/weather`
