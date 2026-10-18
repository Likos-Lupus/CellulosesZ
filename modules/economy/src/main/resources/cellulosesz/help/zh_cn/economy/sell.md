# /sell

按设定价值把物品卖回服务器。

## 行为

- `/sell hand [amount]`：出售手中的物品。
- `/sell <item> [amount]`：出售主背包中匹配的物品。
- `/sell inventory`：出售主背包中所有已定价物品（批量）。
- `/sell blocks`：出售你携带的所有已定价方块物品（批量）。

系统会在服务器线程上捕获确切的物品堆，写入一条持久化的出售意图，随后移除物品，最后才把款项作为一笔平衡的账本交易
按出售 ID 记账。若记账失败，确切的物品堆会被退还；崩溃最多只会丢失物品，绝不会凭空产生货币。

当 `allowNamedItems` 关闭时，带有自定义名称的物品堆会被拒绝；定向查询中无价值的物品也会被拒绝。批量出售需要
开启 `allowBulk` 或拥有 `cellulosesz.sell.bulk` 权限。出售倍率由匹配到的最高
`cellulosesz.sell.multiplier.<n>` 权限授予。

## 参数

- `amount`：可选的正整数；默认出售全部符合条件的物品。
- `item`：物品 ID，格式为 `namespace:path`，例如 `minecraft:diamond`。

## 示例

- `/sell hand`
- `/sell hand 16`
- `/sell minecraft:diamond 10`
- `/sell inventory`

## 相关命令

- `/worth`、`/setworth`、`/balance`
