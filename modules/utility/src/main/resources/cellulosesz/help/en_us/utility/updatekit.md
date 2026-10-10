# /updatekit

Replace a kit's contents with your current inventory.

## Parameters

- `name`: the kit name.
- `once` / `cooldown <duration>`: optional; change the reuse policy.

## Example

`/updatekit starter`
`/updatekit vip cooldown 12h`

## Failures

- The kit does not exist.
- The duration is invalid.

## Related

- `/createkit`, `/delkit`
