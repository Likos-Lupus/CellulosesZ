# /msg

Send a private message to an online player.

## Behavior

The recipient must be online. Delivery also requires that the recipient is accepting private
messages and has not ignored you. Muted players can't send private messages. The default maximum
length is 512 characters; the server can change it.

## Parameters

- `player`: the recipient's name.
- `message`: the text to send, shown to the recipient as you typed it.

## Example

- `/msg Alice hello there`

## Failures

- The recipient is offline or hidden from you.
- The recipient isn't accepting private messages, or is ignoring you.
- You are muted.
- The message is empty or too long.

## Related commands

- `/reply`, `/msgtoggle`, `/ignore`
