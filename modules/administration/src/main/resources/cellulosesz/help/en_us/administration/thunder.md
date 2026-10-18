# /thunder

Toggle thunder in a world.

## Behavior

`true` turns thunder on, `false` turns it off. An optional duration in seconds schedules the toggle
to revert on its own. The affected world is the one you are standing in.

## Parameters

- `true` or `false`: the desired thunder state.
- `duration`: optional length in seconds.

## Example

- `/thunder true`
- `/thunder true 60`
- `/thunder false`

## Failures

- None; a world without weather is left unchanged.

## Related commands

- `/weather`, `/lightning`, `/time`
