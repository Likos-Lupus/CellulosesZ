# /sell

Sell items back to the server for their configured worth.

## Behavior

- `/sell hand [amount]`: sell from the stack in your hand.
- `/sell <item> [amount]`: sell matching items from your main inventory.
- `/sell inventory`: sell every priced item in your main inventory (bulk).
- `/sell blocks`: sell every priced block item you carry (bulk).

The exact stacks are captured on the server thread, a durable intent is written, the stacks are
removed, and only then is the credit committed as one balanced ledger transaction keyed by the sale
id. If the credit ever fails, the exact stacks are handed back; a crash can at worst lose items, and
never create money.

A renamed stack is refused unless `allowNamedItems` is on, and an item with no worth is refused for
a targeted lookup. Bulk selling requires `allowBulk` or the `cellulosesz.sell.bulk` permission. Sell
price multipliers are awarded by the highest matching `cellulosesz.sell.multiplier.<n>` permission.

## Parameters

- `amount`: an optional positive whole number; defaults to everything eligible.
- `item`: an item id in `namespace:path` form, for example `minecraft:diamond`.

## Example

- `/sell hand`
- `/sell hand 16`
- `/sell minecraft:diamond 10`
- `/sell inventory`

## Related

- `/worth`, `/setworth`, `/balance`
