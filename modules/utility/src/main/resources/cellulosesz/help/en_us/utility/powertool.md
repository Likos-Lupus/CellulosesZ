# /powertool

Bind a command to an item's click.

## Parameters

- `<command>`: bind the command to both clicks.
- `l:<command>`: bind to left-click.
- `r:<command>`: bind to right-click.
- `c:<command>`: bind to both clicks.
- No argument removes the binding for the held item.

## Behavior

Runs the command as yourself when you click with the bound item. A powertool cannot call another
powertool command, and each player is throttled to one firing per second. The interaction is
cancelled when a powertool matches.

## Examples

- `/powertool spawn`
- `/powertool l:home`
- `/powertool r:workbench`

## Failures

- Nothing is held.
- The command re-enters a powertool.
- Powertools are disabled.

## Related commands

- `/powertoollist`, `/powertooltoggle`
