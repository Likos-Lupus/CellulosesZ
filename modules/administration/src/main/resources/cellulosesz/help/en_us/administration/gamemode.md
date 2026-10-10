# /gamemode

Change a player's game mode.

## Parameters

- `mode`: `survival`, `creative`, `adventure` or `spectator`.
- `player`: optional target; defaults to yourself.

## Example

`/gamemode creative`
`/gamemode survival Alice`

## Failures

- The mode is unknown.
- The target is not online (when named).
- The target is protected.

## Related

- `/fly`, `/god`
