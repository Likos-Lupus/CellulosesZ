# /pweather

设置个人天气。

## 行为

在不改变世界天气的前提下查看或设置只属于你的天气。支持 `list`、`reset`、`clear`、`rain` 和 `thunder`。该
设置会在你进入服务器或切换世界时重新下发，并在你离线时清除。

## 参数

- `value`：`clear`、`rain` 或 `thunder`。
- `list`：查看当前个人天气。
- `reset`：恢复为世界天气。
- `player`：可选目标，需要 others 权限；缺省为自己。

## 示例

- `/pweather clear`
- `/pweather rain`
- `/pweather reset`

## 失败情形

- 天气值无法识别。

## 相关命令

- `/ptime`、`/weather`
