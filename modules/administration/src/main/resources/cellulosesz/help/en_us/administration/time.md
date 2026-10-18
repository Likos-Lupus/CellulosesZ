# /time

Show or change the time of a world.

## Behavior

With no arguments, prints the current time. `set` jumps straight to the value, while `add` shifts
the clock forward by it. The optional last argument is `all` (every world), a dimension id, or a
world alias; with nothing there the world you are standing in is used. Worlds whose dimension has a
fixed clock cannot be changed.

Accepts `day`, `noon`, `night`, `midnight`, `dawn`, `dusk`, a 24-hour `17:30`, a 12-hour `4pm`, or a
raw tick count such as `4000ticks`.

## Parameters

- `set`: jump to an absolute time.
- `add`: move the clock forward.
- `value`: the time, chosen from the forms above.
- `scope`: optional `all`, a dimension id, or a world alias.

## Example

- `/time`
- `/time set night`
- `/time set 17:30 all`
- `/time add 1000`

## Failures

- The time value is unrecognized.
- The chosen world uses a fixed clock.

## Related commands

- `/ptime`, `/weather`, `/gc`
