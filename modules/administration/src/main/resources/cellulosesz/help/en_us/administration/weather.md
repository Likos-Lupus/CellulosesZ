# /weather

Change the weather of a world.

## Behavior

Sets the weather to `clear`, `rain` or `thunder`. An optional duration in seconds limits how long
the new weather lasts; without it the normal cycle resumes. The optional last argument is `all`
(every world), a dimension id, or a world alias; with nothing there the world you are standing in is
used.

## Parameters

- `type`: `clear`, `rain` or `thunder`.
- `duration`: optional length in seconds.
- `scope`: optional `all`, a dimension id, or a world alias.

## Example

- `/weather clear`
- `/weather rain 300`
- `/weather thunder 120 all`

## Failures

- The weather type is unrecognized.

## Related commands

- `/time`, `/thunder`, `/pweather`
