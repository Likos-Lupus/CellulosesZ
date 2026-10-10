# /ban

Permanently ban a player account.

## Parameters

- `player`: the player to ban (online or known identity).
- `reason`: optional reason.

## Example

`/ban Alice cheating`

## Behavior

Uses the vanilla account ban list as the source of truth; the entry is written before the player is
disconnected.

## Failures

- The target cannot be resolved.
- The target is protected.
- Missing permission.

## Related

- `/tempban`, `/unban`, `/banip`
