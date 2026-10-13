# /updatekit

Replace a kit's contents.

## Parameters

- `name`: the kit name.
- `once`: optional; set one claim per player.
- `cooldown <duration>`: optional; set a cooldown.

## Behavior

Replaces the kit's items with your current inventory. Without a reuse option the existing reuse rule
is kept. Moderator command.

## Examples

- `/updatekit starter`
- `/updatekit vip cooldown 12h`

## Failures

- The kit doesn't exist.
- Your inventory is empty, or the kit has more items than the limit (default 64).
- The cooldown isn't valid or exceeds the server limit.

## Related commands

- `/createkit`, `/delkit`
