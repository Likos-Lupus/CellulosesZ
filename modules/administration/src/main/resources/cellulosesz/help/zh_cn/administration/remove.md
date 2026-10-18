# /remove

批量移除实体。

## 行为

移除符合条件的实体并反馈数量。命名的选择器有 `all`、`tamed`、`named`、`drops`、`arrows`、`boats`、
`minecarts`、`xp`、`paintings`、`itemframes`、`endercrystals`、`monsters`、`animals`、`ambient` 
和 `mobs`；其他词会被当作实体类型 ID 解析。玩家永远不会被移除。最后的可选参数可以是玩家周围的半径方块数，或
单词 `world`；省略时搜索整个世界。

## 参数

- `selector`：命名选择器或实体类型 ID。
- `scope`：可选，半径方块数或 `world`。

## 示例

- `/remove drops`
- `/remove monsters 32`
- `/remove minecraft:arrow world`

## 失败情形

- 选择器无法识别。
- 半径缺失、为零、为负，或超过配置上限。

## 相关命令

- `/spawnmob`、`/spawner`
