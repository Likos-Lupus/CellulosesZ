# /ban

Permanently ban a player account.

## Behavior

Uses the vanilla account ban list, so the ban is enforced on join and survives restarts. The target
can be an online player or a known account name. The entry is written before the player is
disconnected. Bans are recorded in the audit log.

## Parameters

- `player`: the player to ban, online or a known account name.
- `reason`: optional reason, written by you.

## Example

- `/ban Alice cheating`

## Failures

- No account record exists for the name.
- The target is protected.
- Missing permission.

## Related commands

- `/tempban`, `/unban`, `/banip`
