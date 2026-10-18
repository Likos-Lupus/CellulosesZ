# /unlimited

Manage per-item unlimited use.

## Parameters

- `list [player]`: show the unlimited items.
- `clear [player]`: remove every unlimited item.
- `item [player]`: toggle the held item for that player.

## Behavior

An unlimited item the player already holds is topped back up to 2 on every tick; larger stacks are
never reduced. The `[player]` form needs the others permission. Admin command.

## Examples

- `/unlimited item`
- `/unlimited list`
- `/unlimited clear Alice`

## Failures

- Nothing is held for `item`.
- The named player isn't online.
- The storage is temporarily unavailable.

## Related commands

- `/give`, `/item`
