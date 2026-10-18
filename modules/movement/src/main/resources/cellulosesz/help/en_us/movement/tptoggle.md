# /tptoggle

Control whether you accept teleport requests.

## Usage

- `/tptoggle`
- `/tptoggle <on|off>`
- `/tptoggle <player> [<on|off>]`

## Behavior

While your setting is off, nobody can send you a `/tpa`, `/tpahere`, or `/tpaall` request, and
direct
`/tp` and `/tphere` moves that would involve you are refused. Staff holding the bypass capability
can still move you. The setting is stored per account and keeps its value after a restart. With no
argument, your current state is shown.

## Parameters

- `player`: view or change another account's setting; needs the `cellulosesz.tptoggle.others` node.
- `on`: accept teleport requests.
- `off`: refuse teleport requests.

## Example

- `/tptoggle`
- `/tptoggle off`
- `/tptoggle Alex on`

## Failures

- The teleport setting can't be read or saved right now.
- The named account isn't online.

## Related commands

- `/tpa`, `/tpahere`, `/tp`, `/tphere`, `/tpauto`
