# /itemdb

Look up an item's registry id, tags, component summary and durability.

## Parameters

- `item`: optional vanilla item id; when omitted, your held item is used and its durability is
  included.

## Behavior

Shows the display name, registry id, item tags, the non-default data components and the maximum
stack size. Held-item durability is only shown when no item argument is given.

## Examples

- `/itemdb`
- `/itemdb minecraft:diamond_sword`

## Failures

- No item argument and nothing held.

## Related commands

- `/give`, `/item`, `/recipe`
