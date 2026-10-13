# /me

Broadcast an action message about yourself.

## Usage

- `/me <action>`

## Behavior

The action text is shown to players in your current dimension by default. The text is shown exactly
as you type it, and you also see the line. CellulosesZ replaces the vanilla command with the same
limits described below; `/czme` is an alias that always works.

Players you ignore, and players you cannot see (such as vanished staff), do not receive your action
line.

## Limits

By default an action is at most 256 characters, and you can send 8 per minute. These come from
config and can change.

## Failures

- Action messages are disabled.
- The action is empty or too long.
- You're sending action messages too quickly.
- You're muted.

## Related commands

- `/msg`, `/broadcast`
