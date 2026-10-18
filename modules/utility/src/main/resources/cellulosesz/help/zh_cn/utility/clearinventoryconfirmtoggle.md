# /clearinventoryconfirmtoggle

控制 `/clearinventory` 在清空背包前是否要求输入确认码。

## 用法

- `/clearinventoryconfirmtoggle`
- `/clearinventoryconfirmtoggle <on|off>`

## 行为

开启后，`/clearinventory` 和 `/clearinventory target ...` 会先显示预览和确认码，确认码在短时间内有效；输入
确认码前不会清除任何物品。关闭后，这些命令会立即清除。设置按账号保存，重启后仍然生效。不带参数时显示当前状态。

## 参数

- `on`：要求输入确认码。
- `off`：立即清除。

## 示例

- `/clearinventoryconfirmtoggle`
- `/clearinventoryconfirmtoggle off`

## 失败情况

- 暂时无法读取或保存该设置。

## 相关命令

- `/clearinventory`
