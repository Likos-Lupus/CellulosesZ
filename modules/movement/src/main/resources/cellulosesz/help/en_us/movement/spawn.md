# /spawn

Teleport to the server spawn.

## Behavior

Uses the configured spawn when one is set, otherwise the vanilla world spawn. Runs through the
normal teleport pipeline.

## Example

`/spawn`

## Failures

- A teleport restriction is active.
- The destination is unsafe.

## Related

- `/setspawn`, `/delspawn`
