# /enderchest

Open your ender chest, or another player's.

## Parameters

- `player`: optional; open that player's ender chest instead of your own.

## Behavior

Opens an ender chest backed by the real storage, so changes are saved as usual. Opening another
player's ender chest is the same storage a placed ender chest would use. Opening someone else's
ender chest needs the others permission.

## Examples

- `/enderchest`
- `/enderchest Alice`

## Failures

- The named player isn't online.
- Missing permission for another player's ender chest.

## Related commands

- `/disposal`, `/invsee`
