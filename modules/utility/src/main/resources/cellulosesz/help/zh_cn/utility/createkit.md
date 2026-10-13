# /createkit

用背包中的物品创建礼包。

## 参数

- `name`：礼包名称。
- `once`：可选，每人限领一次。
- `cooldown <duration>`：可选，每隔该时长可领取一次。

## 行为

把背包中的物品保存为新礼包。未指定领取规则时不限领取次数。名称使用小写字母、数字、`_` 或 `-`，最长 32
个字符。 管理员命令。

## 示例

- `/createkit starter`
- `/createkit daily once`
- `/createkit vip cooldown 1d`

## 失败情形

- 名称不符合要求或已被使用。
- 背包为空，或物品数量超过上限（默认 64）。
- 礼包数量已达到上限（默认 128）。
- 冷却时长不符合要求或超过服务器上限。

## 相关命令

- `/updatekit`、`/delkit`、`/kitreset`
