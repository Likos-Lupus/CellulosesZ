# /tpaall

Ask every eligible online player to teleport to you.

## Behavior

Sends a `/tpahere`-style request to each eligible online player. A recipient who accepts teleports
to you; you stay where you are. Every recipient decides on their own request.

Vanished players are skipped. The same per-target queue limit as `/tpahere` applies, and a batch is
capped at `maxBatchRecipients` recipients (16 by default). One player may hold up to
`maxOutgoingPerPlayer` outgoing requests at once (16 by default). After a batch,
`batchCooldownSeconds`
must pass before another batch (120 seconds by default).

## Example

- `/tpaall`

## Limits and cautions

- Recipient cap, outgoing cap and batch cooldown come from `[movement.requests]` and can be changed
  by the server.
- A player whose request queue is already full is skipped.
- Nothing is auto-accepted on the recipient's side; recipients still decide.

## Failures

- No eligible player is online.
- You are still on the batch cooldown.
- You already have too many outgoing requests.
- The server has too many pending requests.

## Related

- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/tpauto`
