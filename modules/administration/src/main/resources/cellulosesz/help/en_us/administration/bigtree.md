# /bigtree

Grow a large tree at the block above the block you are looking at.

## Behavior

Like `/tree`, but kinds with a large variant use it: `tree` (fancy oak), `redwood`/`spruce` (mega
spruce), `pine` (mega pine), `birch` (tall birch), `jungle` (mega jungle), `darkoak`, `megaredwood`
and `megajungle`. The placement block is one above the looked-at block, must not be solid, and is
limited by `[administration.worldTools].treeCommandRangeLimit`.

## Parameters

- `type`: one of the supported large tree kinds.

## Example

- `/bigtree tree`
- `/bigtree darkoak`
- `/bigtree megajungle`

## Failures

- The tree type is unrecognized.
- You are not looking at a block within the configured range.
- The block above is solid, or the tree cannot grow there.

## Related commands

- `/tree`, `/break`, `/ice`
