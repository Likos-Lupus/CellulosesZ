# /exp

View, give, take, or set experience points.

## Behavior

With no argument, shows your level, total points and points to the next level. `get` shows your own
experience, or another player's when a name is given; `give`, `take` and `set` change it. Every
amount is clamped to the server's maximum adjustment (default 1,000,000), and taking points never
goes below zero. Viewing and changing your own experience needs no extra permission; viewing another
player and the other forms are moderator commands.

## Examples

- `/exp`
- `/exp get`
- `/exp get Alice`
- `/exp give 500`
- `/exp give 500 Alice`
- `/exp take 250 Alice`
- `/exp set 1000 Alice`

## Failures

- The named player isn't online.

## Related commands

- `/speed`
