# /kill

Kill an online player.

## Behavior

Kills the named online player immediately. The target can't be protected, and you can't target
yourself. The action is recorded in the audit log.

## Parameters

- `player`: the online player to kill.

## Example

- `/kill Alice`

## Failures

- The player is not online.
- The target is protected.
- You tried to target yourself.

## Related commands

- `/gamemode`
