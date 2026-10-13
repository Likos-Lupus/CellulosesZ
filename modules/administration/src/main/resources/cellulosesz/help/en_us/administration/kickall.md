# /kickall

Disconnect every online player.

## Behavior

Every online player is disconnected except protected players and the player who ran the command. The
reason is shown on each disconnect screen; when omitted, the server's default kick reason is used.
The command reports how many were kicked and how many were skipped.

## Parameters

- `reason`: optional reason, written by you.

## Example

- `/kickall maintenance`

## Failures

- The reason is too long.

## Related commands

- `/kick`
