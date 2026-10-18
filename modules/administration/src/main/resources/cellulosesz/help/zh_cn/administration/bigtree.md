# /bigtree

在你所看方块的上方种一棵大树。

## 行为

与 `/tree` 类似，但有大型变种的种类会使用大型变种：`tree`（大橡树）、`redwood`/`spruce`（巨型云杉）、
`pine`（巨型松树）、`birch`（高白桦）、`jungle`（巨型丛林树）、`darkoak`、`megaredwood` 和 
`megajungle`。生成位置是所看方块的上一格，不能是固体方块，并受 
`[administration.worldTools].treeCommandRangeLimit` 限制。

## 参数

- `type`：受支持的大型树木种类之一。

## 示例

- `/bigtree tree`
- `/bigtree darkoak`
- `/bigtree megajungle`

## 失败情形

- 树木类型无法识别。
- 你没有看向配置范围内的方块。
- 上方是固体方块，或该处无法生成树木。

## 相关命令

- `/tree`、`/break`、`/ice`
