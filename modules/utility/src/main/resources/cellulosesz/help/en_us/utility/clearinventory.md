# /clearinventory

Clear your own inventory, with a confirmation code.

## Usage

- `/clearinventory`
- `/clearinventory all`
- `/clearinventory confirm <code>`
- `/clearinventory target <player> <main|all>`
- `/clearinventory target-confirm <player> <code>`

## Behavior

Running it without a code previews what would be cleared and issues a short-lived confirmation code
(default 15 seconds). `/clearinventory` previews your main inventory and hotbar; `/clearinventory
all` additionally covers armor and offhand. Run `/clearinventory confirm <code>` to apply the
preview. The code works once, and the preview is cancelled if anything in those slots changes
between the preview and the confirmation.

`target` and `target-confirm` are staff-only (the `clearinventory.others` permission): they preview
and apply the same operation for another online player, who is notified both times. The ender chest,
the cursor stack and other inventories are never touched. This deletes items with no undo.

## Failures

- The confirmation code is wrong or has expired.
- The inventory changed since the preview, so nothing was cleared.

## Related commands

- `/disposal`
