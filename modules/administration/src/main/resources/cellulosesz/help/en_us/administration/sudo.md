# /sudo

Run a command as another player. Console only.

## Parameters

- `player`: the player to act as.
- `command`: the command to run (a leading `/` is optional).

## Example

`/sudo Alice spawn`

## Failures

- No command was provided, or it is too long or contains control characters.
- The player is not online.

## Behavior

Runs from a non-player source only; player sources cannot use this command.

## Related

- `/gamemode`
