# /worth

查看某个物品在服务器上的价值，或你身上物品的总价值。

## 行为

- `/worth`：显示手中物品的单价与总价。
- `/worth hand [amount]`：同上，但使用指定数量（前缀 `-` 可查看负总价）。
- `/worth inventory`：统计主背包中所有已定价物品的总价值。
- `/worth blocks`：统计你所携带的已定价方块物品的总价值。
- `/worth <item> [amount]`：显示指定物品 ID 与数量的单价和总价。

单价是服务器的基础价格；出售倍率仅在 `/sell` 时生效。数量可以大于你实际拥有的数量，系统只按请求的数量计算价格。

## 参数

- `amount`：可选的整数，可为负数；默认使用你实际拥有的数量。
- `item`：可选的物品 ID，格式为 `namespace:path`，例如 `minecraft:diamond`。

## 示例

- `/worth`
- `/worth hand 64`
- `/worth inventory`
- `/worth minecraft:diamond 10`

## 相关命令

- `/sell`、`/setworth`
