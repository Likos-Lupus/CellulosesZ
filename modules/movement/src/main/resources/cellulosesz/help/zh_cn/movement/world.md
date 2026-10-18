# /world

显示当前所在的世界、列出已加载的世界，或传送到某个世界的出生点。

## 行为

- `/world`：显示当前世界及其维度 ID。
- `/world list`：列出所有已加载的世界及其维度 ID。
- `/world <名称>`：传送到该世界的出生点。

`名称` 既可以是配置的别名，也可以是完整维度 ID（例如 `minecraft:the_end`）。别名来自
`[movement.worlds.aliases]`（默认提供 `overworld`、`nether`、`end`）。

## 参数

- `name`：世界别名或维度 ID。

## 示例

- `/world`
- `/world nether`
- `/world minecraft:the_end`
- `/world list`

## 失败情形

- 没有已加载的世界与该名称匹配。
- 目标位置不安全或超出世界边界。
- 已启用逐世界权限，但你无权进入该世界。

## 相关命令

- `/spawn`、`/home`、`/warp`
