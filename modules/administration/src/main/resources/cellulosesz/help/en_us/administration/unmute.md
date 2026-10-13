# /unmute

Lift a mute.

## Behavior

Removes the mute and notifies the player if they are online. Unmutes are recorded in the audit log.

## Parameters

- `player`: the player to unmute.

## Example

- `/unmute Alice`

## Failures

- No account record exists for the name.
- The player is not muted.
- Mute records can't be read or saved right now.

## Related commands

- `/mute`, `/tempmute`, `/muteinfo`
