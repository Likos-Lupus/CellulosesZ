# /tpdeny

Deny a pending teleport request.

## Parameters

- `player`: optional sender name. Needed only when several requests are pending.

## Example

`/tpdeny`
`/tpdeny Alice`

## Failures

- No pending request.
- Several are pending and you did not name one.

## Related

- `/tpa`, `/tpahere`, `/tpaccept`, `/tpcancel`
