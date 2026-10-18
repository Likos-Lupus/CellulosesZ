# /invsee

View a player's inventory.

## Parameters

- `player`: the online player to inspect.
- `equip`: optional; open a 9-slot write-through equipment view instead of the inventory.

## Behavior

Opens the target's inventory. It is a read-only snapshot unless the server enables editable
inspection and you have the edit permission, in which case the menu is backed by the real inventory.
The `equip`
form needs the equip permission. You can never inspect your own inventory. Moderator command.

## Examples

- `/invsee Alice`
- `/invsee Alice equip`

## Failures

- The player isn't online, or is you.
- Missing permission.

## Related commands

- `/enderchest`, `/disposal`
