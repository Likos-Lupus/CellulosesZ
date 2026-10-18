# /enchant

为手中物品附魔。

## 参数

- `enchantment`：附魔 ID，可带或不带 `minecraft:` 命名空间。
- `level`：可选（默认 1）；等级 0 表示移除附魔。
- `player`：可选；改为对该玩家手中的物品附魔。

## 行为

将附魔添加到目标手中物品并替换原有等级。超过原版上限的等级需要 unsafe 权限。管理员命令。

## 示例

- `/enchant sharpness 5`
- `/enchant sharpness 0`
- `/enchant efficiency 3 Alice`

## 失败情形

- 附魔不存在。
- 该物品无法获得此附魔。
- 等级超过上限且缺少 unsafe 权限。
- 目标不在线或手上没有物品。

## 相关命令

- `/itemdb`
