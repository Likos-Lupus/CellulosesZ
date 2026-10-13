# /warp

Teleport to a server warp.

## Parameters

- `name`: the warp name.

## Behavior

Runs through the normal teleport pipeline, so safety checks and any configured delay and cooldown
apply.

## Example

- `/warp market`

## Failures

- The warp does not exist.
- The name is invalid.
- The target dimension is unavailable.
- The destination is unsafe.

## Related

- `/warps`, `/setwarp`, `/delwarp`
