# /fireball

Launch a projectile.

## Behavior

Fires a projectile in the direction you are looking. The default type is `fireball` (a large
fireball); other types are `small`, `large`, `dragon`, `skull`, `arrow`, `egg`, `snowball`,
`expbottle`, `splashpotion`, `lingeringpotion`, `trident` and `windcharge`. Speed defaults to 2 and
is capped by `[administration.worldTools].maxProjectileSpeed`.

Appending `ride` makes you ride the projectile; it needs `cellulosesz.fireball.ride`.

## Parameters

- `type`: optional projectile type; defaults to `fireball`.
- `speed`: optional velocity, 0 to 64, capped by the server limit.
- `ride`: optional; ride the projectile.

## Example

- `/fireball`
- `/fireball small`
- `/fireball windcharge 2`
- `/fireball large 2.5 ride`

## Failures

- The type is unrecognized.
- You do not hold `cellulosesz.fireball.<type>`.
- `ride` is refused without its permission.

## Related commands

- `/firework`, `/nuke`, `/lightning`
