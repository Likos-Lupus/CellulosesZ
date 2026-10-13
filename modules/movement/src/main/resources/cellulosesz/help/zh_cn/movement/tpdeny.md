# /tpdeny

拒绝一个等待处理的传送请求。

## 参数

- `player`：可选的发送者名称；仅当有多个请求时需要用。

## 示例

- `/tpdeny`
- `/tpdeny Alice`

## 失败情形

- 没有等待处理的请求。
- 有多个请求但未指定玩家。

## 相关命令

- `/tpa`、`/tpahere`、`/tpaccept`、`/tpcancel`
