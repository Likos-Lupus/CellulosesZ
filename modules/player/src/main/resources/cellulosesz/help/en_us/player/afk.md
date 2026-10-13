# /afk

Toggle your away-from-keyboard state, optionally with a reason.

## Usage

- `/afk`
- `/afk <reason>`

## Behavior

AFK is per session: it clears when you leave and never survives a restart. Moving or looking around
counts as activity; the server can also mark you AFK automatically after a period of inactivity.

## Limits

Auto AFK, the inactivity delay and the activity thresholds are server settings.

## Related commands

- `/near`, `/ping`
