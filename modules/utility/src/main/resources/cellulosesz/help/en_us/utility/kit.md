# /kit

Claim a kit.

## Parameters

- `name`: the kit name.

## Behavior

Once-only and cooldown kits are tracked durably per player; the claim is reserved before items are
delivered, so a crash never duplicates a kit. If the inventory cannot hold the kit, the configured
overflow policy applies (reject or drop). Magically your inventory is checked before delivery.

## Example

`/kit starter`

## Failures

- The kit does not exist.
- You already claimed a once-only kit, or the cooldown is still running.
- Your inventory is full (reject policy).

## Related

- `/kits`, `/showkit`, `/createkit`
