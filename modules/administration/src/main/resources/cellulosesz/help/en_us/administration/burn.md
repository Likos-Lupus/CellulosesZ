# /burn

Set a target on fire.

## Behavior

Sets the target alight for the requested number of seconds. The duration is capped by
`[administration.worldTools].maxBurnSeconds`.

## Parameters

- `player`: the target.
- `seconds`: how long to burn, 0.1 up to the configured cap.

## Example

- `/burn Alice 5`
- `/burn Bob 2.5`

## Failures

- The target is not online.

## Related commands

- `/ext`, `/nuke`, `/lightning`
