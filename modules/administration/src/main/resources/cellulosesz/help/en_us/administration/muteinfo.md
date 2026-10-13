# /muteinfo

Show a player's active mute.

## Behavior

Displays the target's name, state, reason, who issued the mute, and how much time remains (or that
it is permanent). It works for offline players whose account is known.

## Parameters

- `player`: the player to inspect.

## Example

- `/muteinfo Alice`

## Failures

- The player is not muted.

## Related commands

- `/mute`, `/tempmute`, `/unmute`
