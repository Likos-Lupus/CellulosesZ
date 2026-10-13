# /tempbanip

Temporarily ban an IP address.

## Behavior

Works like `/banip`, but the ban expires after the given duration, bounded by the server's maximum.
The address is enforced on join and the ban is recorded in the audit log.

## Parameters

- `target`: an IP address, or an online player whose address is used.
- `duration`: such as `1d` or `12h`.
- `reason`: optional reason, written by you.

## Example

- `/tempbanip 203.0.113.7 7d`

## Failures

- The address can't be parsed.
- The duration is invalid or exceeds the server limit.
- The address belongs to a protected player.

## Related commands

- `/banip`, `/unbanip`
