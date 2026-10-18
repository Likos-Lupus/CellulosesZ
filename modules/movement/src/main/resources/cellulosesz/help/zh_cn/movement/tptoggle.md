# /tptoggle

控制你是否接收传送请求。

## 用法

- `/tptoggle`
- `/tptoggle <on|off>`
- `/tptoggle <player> [<on|off>]`

## 行为

关闭后，其他玩家无法再向你发送 `/tpa`、`/tpahere`、`/tpaall` 请求，涉及你的直接 `/tp`、`/tphere` 也会被
拒绝。拥有 bypass 权限的管理员仍可传送你。设置按账号保存，重启后仍然生效。不带参数时显示当前状态。

## 参数

- `player`：查看或修改其他账号的设置；需要 `cellulosesz.tptoggle.others` 权限。
- `on`：接收传送请求。
- `off`：拒绝传送请求。

## 示例

- `/tptoggle`
- `/tptoggle off`
- `/tptoggle Alex on`

## 失败情况

- 暂时无法读取或保存传送设置。
- 指定的账号不在线。

## 相关命令

- `/tpa`、`/tpahere`、`/tp`、`/tphere`、`/tpauto`
