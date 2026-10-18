# /list

Show who is online, optionally narrowed to one configured group.

## Usage

- `/list`
- `/list <group>`

## Behavior

Every online account is placed in the first configured group whose permission it holds, falling back
to the default group. Accounts that are AFK are marked with the configured marker. Vanished accounts
are hidden unless you hold `cellulosesz.list.hidden`.

## Related commands

- `/whois`, `/near`
