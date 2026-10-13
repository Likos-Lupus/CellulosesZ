# /kitreset

重置礼包领取记录。

## 参数

- `name`：礼包名称。
- `player`：可选，重置该玩家的记录；省略时重置自己的。

## 行为

清除已记录的领取，使一次性或冷却类礼包可以再次领取。管理员命令。

## 示例

- `/kitreset starter`
- `/kitreset starter Alice`

## 失败情形

- 礼包不存在。
- 找不到该玩家的记录。

## 相关命令

- `/kit`、`/createkit`
