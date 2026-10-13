# /kit

Claim a kit.

## Parameters

- `name`: the kit name.

## Behavior

Delivers the kit into your inventory. Once-only and cooldown kits record the claim for that player
before items are delivered, so a crash can't duplicate a kit. If your inventory can't hold the kit,
the server's overflow policy applies: it either refuses the whole claim (`REJECT`, the default) or
drops what doesn't fit.

## Examples

- `/kit starter`

## Failures

- The kit doesn't exist.
- You already claimed a once-only kit, or its cooldown is still running.
- Your inventory is full and the overflow policy is `REJECT`.

## Related commands

- `/kits`, `/showkit`, `/createkit`
