# /invsee

View a player's inventory.

## Parameters

- `player`: the online player to inspect.

## Behavior

Opens a read-only snapshot of that player's inventory. You can't take, move, or change items, and
shift-click transfers are disabled. Moderator command.

## Examples

- `/invsee Alice`

## Failures

- The player isn't online.
- Missing permission.

## Related commands

- `/enderchest`, `/disposal`
