# /tree

Grow a tree at the block above the block you are looking at.

## Behavior

The tree is placed one block above the looked-at block, inside the distance limit set by
`[administration.worldTools].treeCommandRangeLimit`. The block at the placement position must not be
solid. Supported kinds include `tree`/`oak`, `bigtree`, `redwood`/`spruce`, `tallredwood`,
`megaredwood`, `pine`, `megapine`, `birch`, `tallbirch`, `jungle`, `smalljungle`, `cocoa`,
`junglebush`, `redmushroom`, `brownmushroom`, `swamp`, `acacia`, `darkoak`, `megajungle`,
`crimsonfungus`, `warpedfungus`, `cherry`, `mangrove`, `tallmangrove`, `azalea` and `paleoak`.

## Parameters

- `type`: one of the supported tree kinds.

## Example

- `/tree tree`
- `/tree acacia`
- `/tree cherry`

## Failures

- The tree type is unrecognized.
- You are not looking at a block within the configured range.
- The block above is solid, or the tree cannot grow there.

## Related commands

- `/bigtree`, `/break`, `/ice`
