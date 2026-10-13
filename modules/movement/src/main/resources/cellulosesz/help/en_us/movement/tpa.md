# /tpa

Request to teleport to another player.

## Behavior

Sends a teleport request. The target accepts with `/tpaccept` or denies with `/tpdeny`; the move
only happens after an accept. You can keep several outgoing requests at once, up to the server limit
(16 by default), and asking the same target again refreshes that request. Requests expire after 60
seconds (default). Use `/tpaall` to ask every online player.

## Parameters

- `player`: the player you want to teleport to.

## Example

- `/tpa Alice`

## Failures

- The target is not online.
- You already have too many outgoing requests (16 by default).
- The target has too many pending requests (5 by default).
- The server has too many pending requests.

## Related

- `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`
