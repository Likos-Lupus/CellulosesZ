# /payconfirmtoggle

Control whether `/pay` asks you to repeat the command before it moves funds.

## Usage

- `/payconfirmtoggle`
- `/payconfirmtoggle <on|off>`

## Behavior

While confirmation is on, the first `/pay` only prepares the payment; running the identical command
again with the same recipient and amount executes it. Any different command replaces the pending
one, and a toggle, disconnect, or server stop clears it. The setting is stored per account and keeps
its value after a restart. With no argument, the current state is shown.

## Parameters

- `on`: require a repeat before paying.
- `off`: pay immediately.

## Example

- `/payconfirmtoggle`
- `/payconfirmtoggle on`

## Failures

- The setting can't be read or saved right now.

## Related commands

- `/pay`, `/paytoggle`
