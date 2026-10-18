# /ptime

设置个人时间。

## 行为

在不改变世界时间的前提下查看或设置你自己的时间。支持 `list` 和 `reset`，命名时间值（`day`、`noon`、`night`、
`midnight`、`dawn`、`dusk`），钟点时间（如 `17:30` 或 `4pm`），以及游戏刻数（如 `4000ticks`）。在值前
加 `@` 可为你在该时间冻结时钟。该设置会在你进入服务器或切换世界时重新下发，并在你离线时清除。

## 参数

- `value`：时间值，可加 `@` 前缀表示冻结。
- `list`：查看当前个人时间。
- `reset`：恢复为世界时间。
- `player`：可选目标，需要 others 权限；缺省为自己。

## 示例

- `/ptime day`
- `/ptime @night`
- `/ptime reset`
- `/ptime list`

## 失败情形

- 时间值无法识别。

## 相关命令

- `/pweather`、`/time`
