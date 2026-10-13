# /spawn

Teleport to the server spawn.

## Behavior

Uses the configured server spawn when one is set, otherwise the vanilla world spawn. Runs through
the normal teleport pipeline.

## Example

- `/spawn`

## Failures

- The target dimension is unavailable.
- The destination is unsafe.
- The teleport is on cooldown (when one is configured).

## Related

- `/setspawn`, `/delspawn`
