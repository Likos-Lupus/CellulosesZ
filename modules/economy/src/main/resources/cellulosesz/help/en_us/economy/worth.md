# /worth

Show what an item is worth to the server, or the total worth of what you carry.

## Behavior

- `/worth`: show the unit price and total for the item in your hand.
- `/worth hand [amount]`: the same, for an explicit count (a leading `-` shows a negative total).
- `/worth inventory`: the combined worth of every priced item in your main inventory.
- `/worth blocks`: the combined worth of every priced block item you carry.
- `/worth <item> [amount]`: the unit price and total for a specific item id and count.

The unit price is the base server price; bonus sell multipliers are applied only by `/sell`. A count
larger than the amount you own is allowed and simply priced as requested.

## Parameters

- `amount`: an optional whole number, negative allowed; defaults to what you own.
- `item`: an optional item id in `namespace:path` form, for example `minecraft:diamond`.

## Example

- `/worth`
- `/worth hand 64`
- `/worth inventory`
- `/worth minecraft:diamond 10`

## Related

- `/sell`, `/setworth`
