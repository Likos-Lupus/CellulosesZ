# /tpa

Request to teleport to another player.

## Behavior

Sends a teleport request. The target must accept it with `/tpaccept` before the move happens. You
may only have one outgoing request; use `/tpcancel` to drop it.

## Parameters

- `player`: the player you want to teleport to.

## Example

`/tpa Alice`

## Failures

- The target is not online.
- You already have a pending request.
- The target has too many pending requests.

## Related

- `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`
