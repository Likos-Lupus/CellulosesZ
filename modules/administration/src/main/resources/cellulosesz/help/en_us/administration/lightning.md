# /lightning

Strike lightning.

## Behavior

Without a target, strikes real lightning at the block you are looking at (up to 600 blocks). With a
target, strikes a visual-only bolt and deals the optional damage; players in god mode are not
damaged. The power argument is damage, not a bolt count.

## Parameters

- `player`: optional target. This branch requires `cellulosesz.lightning.others`.
- `power`: optional damage, 1 to 100; defaults to 5. Requires a target.

## Example

- `/lightning`
- `/lightning Alice`
- `/lightning Alice 3`

## Failures

- You are not looking at a block (no-target form).
- The target is not online.

## Related commands

- `/thunder`, `/nuke`, `/weather`
