# /skull

Create or edit a player head.

## Parameters

- `owner`: optional head owner; defaults to you.
- `player`: optional recipient when spawning a head.

## Behavior

Without the spawn permission the command writes the owner profile onto the held item. With the spawn
permission it creates a new player head for the recipient. Creating a head for another owner needs
the others permission. Moderator command.

## Examples

- `/skull`
- `/skull Notch`
- `/skull Notch Alice`

## Failures

- No head is held and spawning is not permitted.
- The recipient isn't online or their inventory is full.
- Another owner is requested without the others permission.

## Related commands

- `/item`
