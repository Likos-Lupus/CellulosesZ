# /jail

Move a player into a named jail and hold them there.

## Behavior

The sentence is written before the player is moved, so it survives a restart; if the player is
offline they are moved in when they next join. A jailed player can't teleport out and is pulled back
if they leave the jail's allowed area. The jail name is required. The duration is optional and
defaults to `permanent`; a timed sentence is released automatically when it expires. The target can
be an online player or a known account name. Jails are recorded in the audit log. The reason is
stored as written.

## Parameters

- `player`: the player to jail.
- `jail`: the jail to hold them in. It must already exist.
- `duration`: optional sentence length such as `30m`, `1d12h`, or `permanent` (the default).
- `reason`: optional reason, written by you.

## Example

- `/jail Alice cells`
- `/jail Alice cells 1d griefing`

## Failures

- No account record exists for the name.
- The target is protected.
- The player is already jailed.
- The jail name is invalid, or no jail has that name.
- The duration is not a valid length or `permanent`.
- Jail records can't be read or saved right now.
- The jail system is disabled.

## Related commands

- `/unjail`, `/togglejail`, `/setjail`, `/jailedplayers`
