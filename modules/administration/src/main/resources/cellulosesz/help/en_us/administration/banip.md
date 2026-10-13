# /banip

Ban an IP address.

## Behavior

Uses the vanilla IP ban list. The target is either an IP address or an online player, in which case
that player's current address is used. Bans are enforced on join and recorded in the audit log.
Vanilla enforces IPv4 fully; IPv6 coverage depends on the server software, so check the address
before using it.

## Parameters

- `target`: an IP address, or an online player whose address is used.
- `reason`: optional reason, written by you.

## Example

- `/banip 203.0.113.7`
- `/banip Alice`

## Failures

- The address can't be parsed.
- The address belongs to a protected player.
- The address is already banned.
- Missing permission.

## Related commands

- `/tempbanip`, `/unbanip`, `/ban`
