# /more

Increase the stack in your hand.

## Parameters

- `amount`: optional; how many to add. Without it the stack is raised to its maximum size.

## Behavior

Adds up to `amount` to the stack in your main hand, capped at the item's maximum stack size.
Disabled by default on the server. Moderator command.

## Examples

- `/more`
- `/more 16`

## Failures

- Your hand is empty, or the stack is already at its maximum.
- That item action is disabled by the server.

## Related commands

- `/repair`, `/condense`
