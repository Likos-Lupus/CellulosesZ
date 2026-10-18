# /paytoggle

Control whether other accounts can send you payments.

## Usage

- `/paytoggle`
- `/paytoggle <on|off>`
- `/paytoggle <player> [<on|off>]`

## Behavior

While your setting is off, `/pay` to you is refused before the ledger is touched. Staff holding the
bypass capability can still pay you. The setting is stored per account and keeps its value after a
restart. With no argument, your current state is shown.

## Parameters

- `player`: view or change another account's setting; needs the `cellulosesz.paytoggle.others` node.
- `on`: accept payments.
- `off`: refuse payments.

## Example

- `/paytoggle`
- `/paytoggle off`
- `/paytoggle Alex on`

## Failures

- The payment setting can't be read or saved right now.
- The named account isn't online.

## Related commands

- `/pay`, `/balance`, `/payconfirmtoggle`
