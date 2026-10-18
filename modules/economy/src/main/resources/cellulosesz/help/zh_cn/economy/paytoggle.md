# /paytoggle

控制其他账号能否向你付款。

## 用法

- `/paytoggle`
- `/paytoggle <on|off>`
- `/paytoggle <player> [<on|off>]`

## 行为

关闭后，别人向你 `/pay` 会在进入账本前被拒绝。拥有 bypass 权限的管理员仍可向你付款。设置按账号保存，重启后仍然
生效。不带参数时显示当前状态。

## 参数

- `player`：查看或修改其他账号的设置；需要 `cellulosesz.paytoggle.others` 权限。
- `on`：接收付款。
- `off`：拒绝付款。

## 示例

- `/paytoggle`
- `/paytoggle off`
- `/paytoggle Alex on`

## 失败情况

- 暂时无法读取或保存支付设置。
- 指定的账号不在线。

## 相关命令

- `/pay`、`/balance`、`/payconfirmtoggle`
