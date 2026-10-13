# /condense

Condense items into their crafting blocks.

## Behavior

Converts items that have a reversible 2x2 or 3x3 crafting recipe (for example nine ingots into a
block) into the crafted result. Only recipes that craft back into the same item are used, so nothing
is lost; other items are left untouched. Disabled by default on the server.

## Examples

- `/condense`

## Failures

- There's nothing to condense.
- Condensing is disabled by the server.

## Related commands

- `/repair`, `/more`
