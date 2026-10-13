# /unjail

Release a player from jail.

## Behavior

The player is marked for release, then returned to the server spawn. The target can be an online
player or a known account name. The release is recorded in the audit log.

## Parameters

- `player`: the player to release.

## Example

- `/unjail Alice`

## Failures

- No account record exists for the name.
- The player is not jailed.
- Jail records can't be read or saved right now.

## Related commands

- `/jail`, `/togglejail`, `/jailedplayers`
