# /bigtree

在你所看的方块处种一棵大树。

## 行为

与 `/tree` 类似，但每个种类都是大型变种。支持 `tree`（大橡树）、`redwood`（巨型云杉）、`jungle`（巨型丛林树）
和 `darkoak`（深色橡树）。

## 参数

- `type`：受支持的大型树木种类之一。

## 示例

- `/bigtree tree`
- `/bigtree darkoak`
- `/bigtree jungle`

## 失败情形

- 树木类型无法识别。
- 你没有看向任何方块，或该处无法生成树木。

## 相关命令

- `/tree`、`/break`、`/ice`
