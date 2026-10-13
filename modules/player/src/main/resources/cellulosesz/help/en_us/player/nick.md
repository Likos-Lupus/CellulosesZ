# /nick

Set, change or remove your own nickname.

## Usage

- `/nick`
- `/nick set <name>`
- `/nick clear`
- `/nick setfor <player> <name>`
- `/nick clearfor <player>`

## Behavior

Running `/nick` with no arguments shows your current nickname and the available subcommands. `set`
stores a nickname for your account and survives restarts; `clear` removes it. Staff with the
`nick.others` permission can use `setfor` and `clearfor` to change another player's nickname,
including offline players the server knows about.

By default a nickname is not shown in vanilla-rendered display names; CellulosesZ renders it at its
own controlled sites. A server operator can enable the global vanilla override through
`players.nicknames.applyToVanillaDisplay`. Your real account name is still used for commands, so
target it with `/msg`, `/tp` and friends. Use `/realname <nickname>` to find out who owns one.

## Limits

By default a nickname is between 2 and 24 characters. Letters, digits, `_`, `-` and `.` are allowed,
and spaces are allowed by default. A nickname must be unique and can't match another player's
account name. These limits come from config and can change.

## Failures

- Nicknames are disabled.
- The nickname is too short or too long.
- The nickname has characters that aren't allowed.
- The nickname is already in use.
- The nickname matches another player's name.
- The target player is unknown.

## Related commands

- `/realname`
