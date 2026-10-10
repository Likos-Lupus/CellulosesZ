# /banip

Ban an IP address.

## Parameters

- `target`: an IP literal or an online player name (whose address is used).
- `reason`: optional reason.

## Example

`/banip 203.0.113.7`
`/banip Alice`

## Behavior

Uses the vanilla IP ban list. Vanilla itself only enforces IPv4 fully; see the administration ADR
for details.

## Failures

- The address cannot be parsed.
- The targeted online player is protected.
- Missing permission.

## Related

- `/tempbanip`, `/unbanip`, `/ban`
