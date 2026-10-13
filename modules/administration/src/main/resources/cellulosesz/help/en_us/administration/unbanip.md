# /unbanip

Lift an IP ban.

## Behavior

Removes the address from the vanilla IP ban list. Unbans are recorded in the audit log.

## Parameters

- `target`: the IP address to unban.

## Example

- `/unbanip 203.0.113.7`

## Failures

- The address can't be parsed.
- The address is not banned.

## Related commands

- `/banip`, `/tempbanip`
