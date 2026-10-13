# /broadcastworld

向某个维度内的玩家发送公告。

## 行为

先指定维度，再输入消息，只有该维度内的玩家会收到。内容按原样发送。默认最大长度为 1000 个字符，服务器可以调整。

## 参数

- `dimension`：维度 id，例如 `minecraft:overworld`。
- `message`：公告内容。

## 示例

- `/broadcastworld minecraft:overworld 现在开始挖矿活动`

## 失败情形

- 维度 id 不合法。
- 该维度内没有玩家。
- 公告功能已禁用。
- 缺少权限。

## 相关命令

- `/broadcast`
