# /antioch

Spawn primed TNT at the block you are looking at.

## Behavior

Spawns lit TNT at the looked-at block. If you pass a message, the classic two-line Holy Hand Grenade
message is broadcast to every online player; the text itself is not used as an item name. The whole
fun group is disabled with `[administration.fun].enabled = false`.

## Parameters

- `message`: optional; when present, triggers the broadcast.

## Example

- `/antioch`
- `/antioch Hello`

## Failures

- The fun commands are disabled.

## Related commands

- `/beezooka`, `/kittycannon`
