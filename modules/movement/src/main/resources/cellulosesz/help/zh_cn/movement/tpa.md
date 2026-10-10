# /tpa

请求传送到另一名玩家。

## 行为

发送传送请求，目标需用 `/tpaccept` 接受后才会移动。你同时只能有一个待处理请求，可用 `/tpcancel` 取消。

## 参数

- `player`：你想传送到的玩家。

## 示例

`/tpa Alice`

## 失败情形

- 目标不在线。
- 你已有待处理请求。
- 目标待处理请求过多。

## 相关命令

- `/tpahere`、`/tpaccept`、`/tpdeny`、`/tpcancel`
