# /ignore

管理你不想接收其消息的玩家。

## 参数

- `add <player>`：不再接收该玩家的私聊。
- `remove <player>`：重新接收。
- 不填参数时列出你已忽略的玩家。

## 示例

`/ignore`
`/ignore add Alice`
`/ignore remove Alice`

## 失败情形

- 服务器不认识该名称。
- 你试图忽略自己。

## 相关命令

- `/msg`、`/msgtoggle`
