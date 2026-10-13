# /speed

Set a walk and fly speed multiplier.

## Behavior

Multiplies your walking and flying speed by the given value. The multiplier is clamped to the
server's allowed range (default 0.1 to 3.0). The change lasts for the current session and is
restored when you disconnect. Setting another player's speed is a moderator command.

## Examples

- `/speed 1.5`
- `/speed 2.0 Alice`

## Failures

- The named player isn't online.

## Related commands

- `/exp`, `/fly`
