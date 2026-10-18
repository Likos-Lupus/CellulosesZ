# /setworth

Set the price the server pays for an item.

## Behavior

With no item id, sets the worth of the item in your hand. With an item id, sets the worth of that
item. The price is persisted and the in-memory catalog is updated so later `/worth` and `/sell`
calls see it immediately. Each write records the operator as `updated_by`.

Prices are exact two-decimal values, for example `1.50`; a price must be positive.

## Parameters

- `item`: an optional item id in `namespace:path` form; defaults to your held item.
- `price`: the exact unit price, a positive two-decimal decimal.

## Example

- `/setworth 2.50`
- `/setworth minecraft:diamond 100.00`

## Related

- `/worth`, `/sell`
