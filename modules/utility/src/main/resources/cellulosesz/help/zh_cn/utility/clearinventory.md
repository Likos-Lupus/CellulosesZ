# /clearinventory

使用验证码清空自己的背包。

## 用法

- `/clearinventory`
- `/clearinventory all`
- `/clearinventory confirm <验证码>`
- `/clearinventory target <玩家> <main|all>`
- `/clearinventory target-confirm <玩家> <验证码>`

## 行为

不带验证码运行时会预览将要清除的内容，并生成一个短期有效的验证码（默认 15 秒）。`/clearinventory` 预览主背包和
快捷栏；`/clearinventory all` 还会包含盔甲和副手。输入 `/clearinventory confirm <验证码>` 即可执行预览。验证码只能
使用一次；如果预览与确认之间这些槽位发生了变化，本次操作会被取消。

`target` 和 `target-confirm` 仅供员工使用（需要 `clearinventory.others` 权限）：它们会对另一名在线玩家预览并执行同样的
操作，并在两个阶段都通知该玩家。末影箱、光标上的物品以及其他背包永远不会被触碰。此操作不可撤销。

## 失败情形

- 验证码错误或已过期。
- 预览之后背包发生了变化，因此没有清除任何物品。

## 相关命令

- `/disposal`
