# /tempban

Temporarily ban a player account.

## Behavior

Works like `/ban`, but the ban expires after the given duration. The duration is bounded by the
server's maximum; when no maximum is set, any duration is accepted. The entry is written before the
player is disconnected. Temporary bans are recorded in the audit log.

## Parameters

- `player`: the player to ban.
- `duration`: such as `1d`, `12h` or `1w`.
- `reason`: optional reason, written by you.

## Example

- `/tempban Alice 3d griefing`

## Failures

- The duration is invalid or exceeds the server limit.
- No account record exists for the name.
- The target is protected.

## Related commands

- `/ban`, `/unban`, `/tempbanip`
