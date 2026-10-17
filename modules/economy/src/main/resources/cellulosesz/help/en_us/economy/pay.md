# /pay

Send an exact amount to another account.

## Behavior

Transfers the amount from your balance to the recipient's balance in one atomic transaction. The
recipient does not need to be online when offline payments are enabled. Amounts are exact decimals
with at most two fractional digits. Retrying the same command starts a new payment.

## Parameters

- `name`: the recipient account.
- `amount`: a positive exact amount, for example `25` or `25.50`.

## Example

- `/pay Alex 25`
- `/pay Alex 25.50`

## Failures

- The amount is not a valid positive decimal.
- The amount is outside the configured range.
- Your balance is too low.
- The recipient balance would exceed the maximum.
- The recipient is not a known account.
- The recipient is offline and offline payments are disabled.

## Related

- `/balance`, `/transactions`
