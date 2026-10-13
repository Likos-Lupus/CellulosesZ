# /home

Teleport to a saved home.

## Behavior

With no name, uses your default home (`home` by default). With a name, looks up that home. The move
runs through the normal teleport pipeline, so safety checks and any configured delay and cooldown
apply.

## Parameters

- `name`: optional home name. Must match a saved home.

## Example

- `/home`
- `/home base`

## Failures

- The home does not exist.
- The target dimension is unavailable.
- The destination is unsafe.
- The teleport is on cooldown (when one is configured).

## Related

- `/sethome`, `/delhome`, `/homes`
