# /home

Teleport to a saved home.

## Behavior

With no name, uses your default home. With a name, looks up that home. The move goes through the
normal teleport pipeline (delay, cooldown and safety checks from configuration apply).

## Parameters

- `name`: optional home name. Must match a saved home.

## Example

`/home`
`/home base`

## Failures

- The home does not exist.
- The target dimension is unavailable.
- A teleport restriction is active.
- The destination is unsafe.

## Related

- `/sethome`, `/delhome`, `/homes`
