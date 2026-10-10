# /mute

Permanently mute a player.

## Parameters

- `player`: the player to mute.
- `reason`: optional reason.

## Example

`/mute Alice spamming`

## Behavior

The mute is persisted before it takes effect and keeps blocking after a restart. Muted players
cannot chat or send private messages.

## Failures

- The target cannot be resolved.
- The target is protected.

## Related

- `/tempmute`, `/unmute`, `/muteinfo`
