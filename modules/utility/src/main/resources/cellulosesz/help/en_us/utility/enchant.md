# /enchant

Enchant the held item.

## Parameters

- `enchantment`: the enchantment id, with or without the `minecraft:` namespace.
- `level`: optional (default 1); level 0 removes the enchantment.
- `player`: optional; enchant that player's held item.

## Behavior

Adds the enchantment to the target's held item, replacing any existing level. Levels above the
vanilla maximum require the unsafe permission. Moderator command.

## Examples

- `/enchant sharpness 5`
- `/enchant sharpness 0`
- `/enchant efficiency 3 Alice`

## Failures

- The enchantment is unknown.
- The item cannot carry the enchantment.
- The level is above the maximum and the unsafe permission is missing.
- The target isn't online or holds nothing.

## Related commands

- `/itemdb`
