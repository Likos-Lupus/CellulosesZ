# /mute

Permanently mute a player.

## Behavior

A mute blocks chat and private messages. It is stored before it takes effect, so it survives a
restart, and the target is notified with the reason. The target can be an online player or a known
account name. Mutes are recorded in the audit log. The reason is stored as written.

## Parameters

- `player`: the player to mute.
- `reason`: optional reason, written by you.

## Example

- `/mute Alice spamming`

## Failures

- No account record exists for the name.
- The target is protected.
- The player is already muted.
- Mute records can't be read or saved right now.

## Related commands

- `/tempmute`, `/unmute`, `/muteinfo`
