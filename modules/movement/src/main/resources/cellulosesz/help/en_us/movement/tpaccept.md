# /tpaccept

Accept a pending teleport request.

## Parameters

- `player`: optional sender name; needed only when several requests are pending.

## Behavior

The requester is then moved according to the request type. Accepting runs the same teleport
pipeline, so safety checks and any configured delay apply.

## Example

- `/tpaccept`
- `/tpaccept Alice`

## Failures

- No request is pending.
- Several are pending and you did not name one.

## Related

- `/tpa`, `/tpahere`, `/tpdeny`, `/tpcancel`
