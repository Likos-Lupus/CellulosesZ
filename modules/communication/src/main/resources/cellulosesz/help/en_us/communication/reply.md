# /reply

Reply to the player you last exchanged private messages with.

## Aliases

- `/r`

## Behavior

The target is the player from your most recent private-message delivery. If the server sets a reply
timeout, an older conversation no longer counts. Delivery follows the same rules as `/msg`: the
recipient must be online and accepting messages, and you must not be muted.

## Parameters

- `message`: the text to send.

## Example

- `/r see you soon`

## Failures

- There is no recent conversation, or it has expired.
- The recipient is offline, not accepting messages, or ignoring you.
- You are muted.

## Related commands

- `/msg`, `/msgtoggle`
