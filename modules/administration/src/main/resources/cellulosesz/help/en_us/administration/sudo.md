# /sudo

Run a command as another player. Console only.

## Behavior

Executes a command exactly as if the named online player had run it, with that player's permissions.
A leading `/` is optional. The command is limited to 256 characters and may not contain control
characters. Because it impersonates a real player, `/sudo` is available only from the console, never
from a player account, and it is recorded in the audit log.

## Parameters

- `player`: the online player to act as.
- `command`: the command to run.

## Example

- `/sudo Alice spawn`

## Failures

- No command was provided, or it is too long or contains control characters.
- The player is not online.
- The target is protected.

## Related commands

- `/gamemode`
