# /createkit

用背包中的物品创建礼包。

## 参数

- `name`：礼包名称。
- `once`：可选，每名玩家只能领取一次。
- `cooldown <duration>`：可选，在时长内只能领取一次。

## 示例

`/createkit starter`
`/createkit daily once`
`/createkit vip cooldown 1d`

## 失败情形

- 名称非法或已被使用。
- 礼包超过配置的物品上限。

## 相关命令

- `/updatekit`、`/delkit`、`/kitreset`
