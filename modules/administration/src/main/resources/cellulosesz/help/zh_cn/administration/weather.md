# /weather

修改世界天气。

## 行为

将天气设为 `clear`（晴朗）、`rain`（降雨）或 `thunder`（雷暴）。可选的持续时间以秒为单位，限制新天气维持
多久；省略时按正常循环恢复。最后的可选参数可以是 `all`（所有世界）、维度 ID 或世界别名；省略时作用于你所在的
世界。

## 参数

- `type`：`clear`、`rain` 或 `thunder`。
- `duration`：可选，持续秒数。
- `scope`：可选，`all`、维度 ID 或世界别名。

## 示例

- `/weather clear`
- `/weather rain 300`
- `/weather thunder 120 all`

## 失败情形

- 天气类型无法识别。

## 相关命令

- `/time`、`/thunder`、`/pweather`
