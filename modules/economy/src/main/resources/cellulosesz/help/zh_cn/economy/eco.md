# /eco

经济管理命令。

## 行为

- `/eco info`：显示货币名称与余额上限。
- `/eco give <name> <amount> [reason]`：由系统发放货币。
- `/eco take <name> <amount> [reason]`：由系统回收货币。
- `/eco set <name> <amount> [reason]`：把账户余额设为精确目标值。
- `/eco history <name> [page]`：审计账户的交易记录。

每次操作都会记录操作者、目标、金额或目标值、原因码以及唯一交易编号。回收金额超过余额时直接拒绝，绝不截断。

## 参数

- `name`：目标账户。
- `amount`：正的精确金额，例如 `100` 或 `100.50`。
- `reason`：可选的原因码。
- `page`：可选页码，从 1 开始。

## 示例

- `/eco give Alex 100`
- `/eco take Alex 25.50`
- `/eco set Alex 0`
- `/eco history Alex`
- `/eco info`

## 相关命令

- `/balance`、`/pay`
