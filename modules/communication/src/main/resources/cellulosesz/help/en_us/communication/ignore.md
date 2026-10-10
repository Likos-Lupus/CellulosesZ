# /ignore

Manage the players whose messages you do not want to receive.

## Parameters

- `add <player>`: stop receiving that player's private messages.
- `remove <player>`: start receiving them again.
- With no argument, lists the players you ignore.

## Example

`/ignore`
`/ignore add Alice`
`/ignore remove Alice`

## Failures

- The name is not known to the server.
- You tried to ignore yourself.

## Related

- `/msg`, `/msgtoggle`
