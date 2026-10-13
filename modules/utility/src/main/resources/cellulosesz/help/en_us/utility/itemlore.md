# /itemlore

Edit the lore (description lines) of the item in your hand.

## Behavior

Replaces the held item's lore. Separate lines with `|`. With no text, clears the lore. The server
limits the number of lines (default 10) and each line's length (default 80 characters).

## Examples

- `/itemlore Found in the deep mines`
- `/itemlore Line one|Line two`
- `/itemlore`

## Failures

- You're not holding an item.
- Too many lore lines, or a line is too long.

## Related commands

- `/itemname`
