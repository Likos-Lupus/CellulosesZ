# /give

Give items to a player.

## Parameters

- `player`: the online recipient.
- `item`: a vanilla item id in `id[component=value,...]` form.
- `amount`: optional; defaults to the configured default and is capped at the configured maximum.

## Behavior

Adds whole stacks to the recipient's inventory. When the server allows it, anything that does not
fit is dropped at their feet; otherwise the command fails without granting anything. Blacklisted
item ids are refused. Moderator command.

## Examples

- `/give Alice minecraft:diamond 16`
- `/give Alice minecraft:netherite_sword[unbreakable={}]`

## Failures

- The named player isn't online.
- The item is blacklisted.
- The amount is outside the server limit.
- The inventory is full and dropping is disabled.

## Related commands

- `/item`, `/itemdb`
