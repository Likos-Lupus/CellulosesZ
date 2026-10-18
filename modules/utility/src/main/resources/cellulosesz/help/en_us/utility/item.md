# /item

Give yourself an item.

## Parameters

- `item`: a vanilla item id in `id[component=value,...]` form.
- `amount`: optional; defaults to the configured default and is capped at the configured maximum.

## Behavior

Adds whole stacks to your inventory, with the same blacklist, cap and drop policy as `/give`.
Moderator command.

## Examples

- `/item minecraft:stone 64`

## Failures

- The item is blacklisted.
- The amount is outside the server limit.
- Your inventory is full and dropping is disabled.

## Related commands

- `/give`, `/itemdb`
