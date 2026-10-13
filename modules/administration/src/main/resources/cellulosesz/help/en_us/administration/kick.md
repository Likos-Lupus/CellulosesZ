# /kick

Disconnect an online player.

## Behavior

Kicks only the named online player. The reason is shown on the target's disconnect screen; when
omitted, the server's default kick reason is used. Protected targets can't be kicked. The kick is
recorded in the audit log.

## Parameters

- `player`: the online player to kick.
- `reason`: optional reason, written by you.

## Example

- `/kick Alice`
- `/kick Alice spamming`

## Failures

- The player is not online.
- The target is protected.
- The reason is too long.

## Related commands

- `/kickall`, `/ban`, `/mute`
