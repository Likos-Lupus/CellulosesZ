# /tempmute

Temporarily mute a player.

## Behavior

Works like `/mute`, but the mute ends when the duration passes. The duration is bounded by the
server's maximum. The mute is stored before it takes effect and is recorded in the audit log.

## Parameters

- `player`: the player to mute.
- `duration`: such as `1h` or `2d`.
- `reason`: optional reason, written by you.

## Example

- `/tempmute Alice 2h`

## Failures

- The duration is invalid or exceeds the server limit.
- No account record exists for the name.
- The target is protected.
- Mute records can't be read or saved right now.

## Related commands

- `/mute`, `/unmute`, `/muteinfo`
