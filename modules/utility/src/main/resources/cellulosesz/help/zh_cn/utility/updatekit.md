# /updatekit

替换礼包内容。

## 参数

- `name`：礼包名称。
- `once`：可选，改为每人限领一次。
- `cooldown <duration>`：可选，改为按冷却领取。

## 行为

把礼包中的物品替换为你当前的背包。未指定领取规则时保留原规则。管理员命令。

## 示例

- `/updatekit starter`
- `/updatekit vip cooldown 12h`

## 失败情形

- 礼包不存在。
- 背包为空，或物品数量超过上限（默认 64）。
- 冷却时长不符合要求或超过服务器上限。

## 相关命令

- `/createkit`、`/delkit`
