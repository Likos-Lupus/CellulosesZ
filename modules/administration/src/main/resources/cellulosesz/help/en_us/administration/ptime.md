# /ptime

Set a personal time.

## Behavior

Shows or sets your own time without changing the world's clock. Accepts `list` and `reset`, the
named time values (`day`, `noon`, `night`, `midnight`, `dawn`, `dusk`), a clock time such as `17:30`
or `4pm`, or a tick count such as `4000ticks`. Prefix the value with `@` to freeze the clock for you
at that time. The override is re-sent whenever you join or change world, and cleared when you quit.

## Parameters

- `value`: a time value, optionally prefixed with `@` to freeze it.
- `list`: show your current personal time.
- `reset`: return to the world's clock.
- `player`: optional target for the `others` permission; defaults to yourself.

## Example

- `/ptime day`
- `/ptime @night`
- `/ptime reset`
- `/ptime list`

## Failures

- The time value is unrecognized.

## Related commands

- `/pweather`, `/time`
