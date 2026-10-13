# /ignore

Manage the list of players whose private messages you don't want to receive.

## Behavior

Ignoring someone blocks their private messages to you. The list is stored per player and keeps its
value after a restart. You can't ignore yourself, and the name must belong to a known player. The
default list size is 128; the server can change it.

## Subcommands

- `/ignore`: list the players you ignore.
- `/ignore add <player>`: stop receiving that player's private messages.
- `/ignore remove <player>`: receive them again.

## Example

- `/ignore`
- `/ignore add Alice`
- `/ignore remove Alice`

## Failures

- No record exists for the name.
- You tried to ignore yourself.
- Your ignore list is full.

## Related commands

- `/msg`, `/msgtoggle`
