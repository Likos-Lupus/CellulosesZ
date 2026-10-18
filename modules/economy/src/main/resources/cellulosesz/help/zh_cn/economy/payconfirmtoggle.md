# /payconfirmtoggle

控制 `/pay` 在转移资金前是否要求你重复输入命令。

## 用法

- `/payconfirmtoggle`
- `/payconfirmtoggle <on|off>`

## 行为

开启后，第一次 `/pay` 只会准备这次付款；再次输入完全相同（收款人和金额都相同）的命令才会执行。输入其他命令会替换
待确认的内容；切换设置、离线或服务器关闭都会清除它。设置按账号保存，重启后仍然生效。不带参数时显示当前状态。

## 参数

- `on`：付款前要求再次确认。
- `off`：直接付款。

## 示例

- `/payconfirmtoggle`
- `/payconfirmtoggle on`

## 失败情况

- 暂时无法读取或保存该设置。

## 相关命令

- `/pay`、`/paytoggle`
