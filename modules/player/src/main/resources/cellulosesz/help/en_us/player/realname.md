# /realname

Show which player owns a nickname.

## Usage

- `/realname <nickname>`

## Behavior

Resolves a stored nickname to the account name that set it. The lookup is case-insensitive and
ignores extra spaces.

## Failures

- No player uses that nickname.
- Player data is temporarily unavailable.

## Related commands

- `/nick`
