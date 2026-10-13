# /repair

Repair items.

## Parameters

- `hand` (default) or `all`.
- `player`: optional; repair that player's items instead of your own.

## Behavior

Restores the affected items to full durability. `hand` repairs the item in your main hand; `all`
repairs your inventory and offhand, and your armor when the server includes it. Enchanted items are
skipped unless the server allows repairing them. Moderator command.

## Examples

- `/repair`
- `/repair all`
- `/repair hand Alice`

## Failures

- The named player isn't online.
- Nothing needs repairing.
- Item repair is disabled by the server.

## Related commands

- `/more`, `/condense`
