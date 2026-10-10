# /createkit

Create a kit from the items in your inventory.

## Parameters

- `name`: the kit name.
- `once`: optional; the kit may be claimed only once per player.
- `cooldown <duration>`: optional; claimed at most once per duration.

## Example

`/createkit starter`
`/createkit daily once`
`/createkit vip cooldown 1d`

## Failures

- The name is invalid or already taken.
- The kit exceeds the configured item limit.

## Related

- `/updatekit`, `/delkit`, `/kitreset`
