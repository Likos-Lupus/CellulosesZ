# /kitreset

Reset kit claims.

## Parameters

- `name`: the kit name.
- `player`: optional; reset that player's claim instead of your own.

## Behavior

Clears the recorded claim so a once-only or cooldown kit can be claimed again. Moderator command.

## Examples

- `/kitreset starter`
- `/kitreset starter Alice`

## Failures

- The kit doesn't exist.
- The named player is unknown to this server.

## Related commands

- `/kit`, `/createkit`
