# /tree

在你所看方块的上方种树。

## 行为

树木生成在你所看方块的上一格，距离受 `[administration.worldTools].treeCommandRangeLimit` 限制，且生成位置
不能是固体方块。支持的种类包括 `tree`/`oak`、`bigtree`、`redwood`/`spruce`、`tallredwood`、
`megaredwood`、`pine`、`megapine`、`birch`、`tallbirch`、`jungle`、`smalljungle`、`cocoa`、
`junglebush`、`redmushroom`、`brownmushroom`、`swamp`、`acacia`、`darkoak`、`megajungle`、
`crimsonfungus`、`warpedfungus`、`cherry`、`mangrove`、`tallmangrove`、`azalea` 和 `paleoak`。

## 参数

- `type`：受支持的树木种类之一。

## 示例

- `/tree tree`
- `/tree acacia`
- `/tree cherry`

## 失败情形

- 树木类型无法识别。
- 你没有看向配置范围内的方块。
- 上方是固体方块，或该处无法生成树木。

## 相关命令

- `/bigtree`、`/break`、`/ice`
