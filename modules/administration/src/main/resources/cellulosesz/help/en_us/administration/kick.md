# /kick

Disconnect an online player.

## Parameters

- `player`: the player to kick.
- `reason`: optional reason; a configured default is used when omitted.

## Example

`/kick Alice`
`/kick Alice spamming`

## Failures

- The player is not online.
- The target is protected (operator protection).
- The reason is invalid.

## Related

- `/kickall`, `/ban`, `/mute`
