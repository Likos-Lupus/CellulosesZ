# /togglejail

Jail a player, or release them if they are already jailed.

## Behavior

Jails a free player using the configured default jail, or marks a jailed player for release. The
reason applies only when jailing. The target can be an online player or a known account name. The
action is recorded in the audit log. Releasing is always available; only jailing a free player is
blocked while the jail system is disabled.

## Parameters

- `player`: the player to toggle.
- `reason`: optional reason used when jailing, written by you.

## Example

- `/togglejail Alice griefing`

## Failures

- No account record exists for the name.
- The target is protected.
- No default jail is set when jailing.
- The jail system is disabled when jailing.
- Jail records can't be read or saved right now.

## Related commands

- `/jail`, `/unjail`, `/setjail`
