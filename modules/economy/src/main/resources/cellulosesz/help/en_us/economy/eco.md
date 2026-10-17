# /eco

Administrative economy control.

## Behavior

- `/eco info`: show the currency name and the maximum balance.
- `/eco give <name> <amount> [reason]`: grant currency from the system.
- `/eco take <name> <amount> [reason]`: reclaim currency into the system.
- `/eco set <name> <amount> [reason]`: set an account balance to an exact target.
- `/eco history <name> [page]`: audit an account's transactions.

Each operation is recorded with the operator, the target, the amount or target value, a reason code
and a unique transaction id. Taking more than the balance allows is rejected; it is never clamped.

## Parameters

- `name`: the target account.
- `amount`: a positive exact amount, for example `100` or `100.50`.
- `reason`: an optional short reason code.
- `page`: an optional page number, starting at 1.

## Example

- `/eco give Alex 100`
- `/eco take Alex 25.50`
- `/eco set Alex 0`
- `/eco history Alex`
- `/eco info`

## Related

- `/balance`, `/pay`
