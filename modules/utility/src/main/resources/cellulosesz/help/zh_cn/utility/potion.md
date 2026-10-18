# /potion

编辑手中药水或施加其效果。

## 参数

- `apply`：对自己饮用手中药水的效果。
- `clear`：清除手中药水的效果。
- `effect:<id>[:seconds[:amplifier]]`：添加一个效果（默认 30 秒，等级 0）。
- `player`：对 `clear` 与 `effect:` 可选；改为编辑该玩家手中的药水。

## 行为

编辑手中药水的 `POTION_CONTENTS` 组件。对自己施加需要 apply 权限；编辑其他玩家需要 others 权限。 管理员命令。

## 示例

- `/potion effect:speed`
- `/potion effect:regeneration:10:1`
- `/potion clear`
- `/potion apply`

## 失败情形

- 手中物品不是药水。
- 效果不存在。

## 相关命令

- `/item`
