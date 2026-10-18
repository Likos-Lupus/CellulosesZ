# /unlimited

管理按物品的无限使用。

## 参数

- `list [player]`：显示无限物品。
- `clear [player]`：移除所有无限物品。
- `item [player]`：为该方法玩家切换手中物品。

## 行为

玩家已持有的无限物品会在每个 tick 补回至 2；数量更大的堆叠不会减少。`[player]` 形式需要 others 权限。
管理员命令。

## 示例

- `/unlimited item`
- `/unlimited list`
- `/unlimited clear Alice`

## 失败情形

- `item` 时手上没有物品。
- 指定玩家不在线。
- 存储暂时不可用。

## 相关命令

- `/give`、`/item`
