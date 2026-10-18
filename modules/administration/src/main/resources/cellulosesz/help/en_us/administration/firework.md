# /firework

Edit or launch fireworks.

## Behavior

The first form sets the flight power of the firework rocket in your hand. `clear` empties its
explosions while keeping the rocket. `fire` launches rockets from your position.

`fire` accepts either an integer amount or a direction word. An integer launches that many rockets
upward, capped by `[administration.worldTools].maxFireworkAmount`. Any non-integer (for example
`north`) launches one rocket in the direction you are looking, with power capped at 1. The `fire`
form needs `cellulosesz.firework.fire`.

## Parameters

- `power`: flight power for the held rocket, capped by
  `[administration.worldTools].maxFireworkPower`.
- `clear`: remove every explosion from the held rocket.
- `fire`: launch rockets.
- `amountOrDirection`: optional integer amount or a direction word.

## Example

- `/firework 2`
- `/firework clear`
- `/firework fire 3`
- `/firework fire north`

## Failures

- You are not holding a firework rocket.
- `fire` is refused without `cellulosesz.firework.fire`.

## Related commands

- `/fireball`, `/nuke`
