# /recipe

Show the crafting recipes for an item.

## Parameters

- `item`: the item whose recipes to look up.
- `page`: optional page number (default 1).

## Behavior

Lists every recipe whose result matches the item, with the required ingredients. Output is
paginated.

## Examples

- `/recipe minecraft:crafting_table`
- `/recipe minecraft:stick 2`

## Failures

- No recipe produces the item.

## Related commands

- `/itemdb`
