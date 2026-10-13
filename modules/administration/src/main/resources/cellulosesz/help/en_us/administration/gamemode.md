# /gamemode

Change a player's game mode.

## Behavior

Sets the named player's game mode. With no name, target yourself. The mode must be one of
`survival`, `creative`, `adventure` or `spectator`. Protected targets can't be changed unless you
are targeting yourself. The change is recorded in the audit log.

## Parameters

- `mode`: `survival`, `creative`, `adventure` or `spectator`.
- `player`: optional target; defaults to yourself.

## Example

- `/gamemode creative`
- `/gamemode survival Alice`

## Failures

- The mode is unknown.
- The target is not online.
- The target is protected.

## Related commands

- `/fly`, `/god`
