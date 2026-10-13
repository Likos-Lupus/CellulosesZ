# /createkit

Create a kit from your inventory.

## Parameters

- `name`: the kit name.
- `once`: optional; one claim per player.
- `cooldown <duration>`: optional; one claim per duration.

## Behavior

Captures the items in your inventory as a new kit. Without a reuse option the kit is unlimited.
Names use lowercase letters, digits, `_` or `-`, up to 32 characters. Moderator command.

## Examples

- `/createkit starter`
- `/createkit daily once`
- `/createkit vip cooldown 1d`

## Failures

- The name isn't valid or is already in use.
- Your inventory is empty, or the kit has more items than the limit (default 64).
- The kit limit has been reached (default 128).
- The cooldown isn't valid or exceeds the server limit.

## Related commands

- `/updatekit`, `/delkit`, `/kitreset`
