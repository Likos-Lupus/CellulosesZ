# /tpdeny

拒绝一个待处理的传送请求。

## 参数

- `player`：可选的发送者名称；仅当有多个待处理请求时需要。

## 示例

`/tpdeny`
`/tpdeny Alice`

## 失败情形

- 没有待处理请求。
- 有多个待处理请求但未指定玩家。

## 相关命令

- `/tpa`、`/tpahere`、`/tpaccept`、`/tpcancel`
