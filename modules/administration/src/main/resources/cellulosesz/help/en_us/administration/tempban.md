# /tempban

Temporarily ban a player account.

## Parameters

- `player`: the player to ban.
- `duration`: bannable duration such as `1d`, `12h` or `1w`.
- `reason`: optional reason.

## Example

`/tempban Alice 3d griefing`

## Failures

- The duration is invalid or exceeds the configured maximum.
- The target is protected.

## Related

- `/ban`, `/unban`, `/tempbanip`
