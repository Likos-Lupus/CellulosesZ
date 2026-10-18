# /rtoggle

Control whether `/reply` reuses your last conversation partner.

## Usage

- `/rtoggle`
- `/rtoggle <on|off>`

## Behavior

While the setting is off, `/reply` has no last recipient and reports that there is no one to reply
to. Turning it back on restores the target recorded by your most recent private-message delivery.
The setting is stored per account and keeps its value after a restart. With no argument, the current
state is shown.

## Parameters

- `on`: reply to the last recipient.
- `off`: stop replying to the last recipient.

## Example

- `/rtoggle`
- `/rtoggle off`

## Failures

- The setting can't be read or saved right now.

## Related commands

- `/reply`, `/msg`
