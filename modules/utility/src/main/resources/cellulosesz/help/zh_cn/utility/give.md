# /give

给予玩家物品。

## 参数

- `player`：在线的接收者。
- `item`：原版物品 ID，形式为 `id[component=value,...]`。
- `amount`：可选；默认取配置的默认值，并受配置上限约束。

## 行为

以整组为单位放入接收者背包。服务器允许时，放不下的部分会掉落在其脚下；否则命令失败且不发放任何物品。黑名单中的
物品会被拒绝。管理员命令。

## 示例

- `/give Alice minecraft:diamond 16`
- `/give Alice minecraft:netherite_sword[unbreakable={}]`

## 失败情形

- 指定玩家不在线。
- 该物品在黑名单中。
- 数量超出服务器上限。
- 背包已满且未开启掉落。

## 相关命令

- `/item`、`/itemdb`
