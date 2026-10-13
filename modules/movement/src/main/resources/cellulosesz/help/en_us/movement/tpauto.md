# /tpauto

Accept teleport requests automatically, and choose who may skip the prompt.

## Behavior

Auto-accept applies only to a request whose direction is "teleport to you" (`/tpa` from another
player); the requester is moved to you. A `/tpahere` request is never auto-accepted, because that
would move you to the requester.

Three modes are available:

- `off`: no request is auto-accepted.
- `trusted`: only players on your trusted list are auto-accepted.
- `all`: every eligible sender is auto-accepted. Ignore and visibility rules still apply.

- `/tpauto` shows the current mode and the available subcommands.
- `/tpauto off`, `/tpauto trusted` and `/tpauto all` set the mode.
- `/tpauto allow <player>` adds a player to the trusted list.
- `/tpauto deny <player>` removes a player from the trusted list.
- `/tpauto list` shows the trusted list.

## Examples

- `/tpauto trusted`
- `/tpauto allow Alice`
- `/tpauto list`
- `/tpauto off`

## Limits and cautions

- `trusted` with an empty list accepts nobody.
- The trusted list size comes from `[movement.requests].maxAutoAllowedPlayers` and can be changed by
  the server.
- Auto-accept only covers `/tpa` (the requester moves to you). `/tpahere` still asks you.
- An entry only skips the prompt for `/tpa`; it grants no other access.

## Failures

- The named player is not online.
- The player is already on the trusted list.
- The trusted list is full.
- The auto-accept setting can't be read or written.

## Related

- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpaall`
