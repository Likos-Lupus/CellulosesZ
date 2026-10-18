# /firework

Edit or launch fireworks.

## Behavior

The first form sets the flight power of the firework rocket in your hand. `clear` empties its
explosions while keeping the rocket. `fire` launches rockets from your position; the optional amount
is capped by the server limit.

## Parameters

- `power`: flight power for the held rocket, capped by
  `[administration.worldTools].maxFireworkPower`.
- `clear`: remove every explosion from the held rocket.
- `fire`: launch rockets.
- `amount`: optional number of rockets, capped by the config.

## Example

- `/firework 2`
- `/firework clear`
- `/firework fire 3`

## Failures

- You are not holding a firework rocket.

## Related commands

- `/fireball`, `/nuke`
