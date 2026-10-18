# /tree

Grow a tree at the block you are looking at.

## Behavior

Places and grows a named tree. The supported kinds are `tree` (oak), `birch`, `redwood` (spruce),
`redmushroom`, `brownmushroom`, `jungle`, `junglebush` and `swamp`. The tree is grown at the block
you are looking at.

## Parameters

- `type`: one of the supported tree kinds.

## Example

- `/tree tree`
- `/tree birch`
- `/tree redmushroom`

## Failures

- The tree type is unrecognized.
- You are not looking at a block, or the tree cannot grow there.

## Related commands

- `/bigtree`, `/break`, `/ice`
