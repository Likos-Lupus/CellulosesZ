# /tp

Teleport yourself to a player, or one player to another.

## Behavior

- `/tp <target>` teleports you to `target`.
- `/tp <from> <to>` teleports `from` to `to` (requires the `tp` permission node; typically a
  moderator action).

## Parameters

- `first`: the moving player when two names are given, otherwise your target.
- `second`: optional destination player when moving someone else.

## Example

`/tp Notch`
`/tp Alice Bob`

## Failures

- A named player is not online.
- You are already at your own position.
- Missing permission for the two-player form.

## Related

- `/tphere`, `/tppos`, `/tpa`
