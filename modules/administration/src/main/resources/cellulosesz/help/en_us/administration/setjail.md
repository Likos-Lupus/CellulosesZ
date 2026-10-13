# /setjail

Create or move a jail location at your current position.

## Behavior

The jail is saved where you stand, including your facing direction. Jailed players are held there.
An existing jail with the same name is replaced, keeping its original creation time, creator and
radius. Jails are shared by the whole server. Creating or moving a jail is blocked while the jail
system is disabled.

## Parameters

- `name`: the jail name. Lowercase letters, digits, `_` or `-`, up to 32 characters.

## Example

- `/setjail cells`

## Limits and cautions

- The number of jails is limited (default 32) and depends on the server setting.

## Failures

- The name is invalid.
- The jail limit was reached.
- The jail system is disabled.
- Jail records can't be read or saved right now.

## Related commands

- `/jail`, `/deljail`, `/jails`
