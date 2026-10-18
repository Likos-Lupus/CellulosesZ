# /clearinventoryconfirmtoggle

Control whether `/clearinventory` asks for a confirmation code before it clears anything.

## Usage

- `/clearinventoryconfirmtoggle`
- `/clearinventoryconfirmtoggle <on|off>`

## Behavior

While confirmation is on, `/clearinventory` and `/clearinventory target ...` first show a preview
and a code that expires after a short window; nothing is cleared until the code is entered. Turning
it off makes those commands clear immediately. The setting is stored per account and keeps its value
after a restart. With no argument, the current state is shown.

## Parameters

- `on`: require the confirmation code.
- `off`: clear immediately.

## Example

- `/clearinventoryconfirmtoggle`
- `/clearinventoryconfirmtoggle off`

## Failures

- The setting can't be read or saved right now.

## Related commands

- `/clearinventory`
