# /cellulosesz permissions

只读权限诊断：显示由哪个后端解析 CellulosesZ 的权限节点，以及针对某个节点的最终结果与原生提供者的意见。

## 子命令

- `/cellulosesz permissions` 或 `/cellulosesz permissions status`：显示当前权限后端，以及是否安装了原生
  LuckPerms 提供者。
- `/cellulosesz permissions check <node>`：检查某个 CellulosesZ 权限节点。

## 行为

最终授权始终来自平台权限 API（Fabric Permissions API 或 NeoForge PermissionAPI）；平台桥接是唯一进行授权的
组件。当安装了原生 LuckPerms 提供者时，命令会额外显示该提供者在玩家当前上下文下的原始三态意见（`allow`、
`deny` 或 `undefined`）。原始 `undefined` 不等于拒绝：平台仍会应用原版回退。

`check` 只接受 CellulosesZ 目录中声明的节点，无法用于探测其他模组的节点。

## 说明

仅限管理员使用。只输出节点名、最终布尔值与原始意见；玩家 IP、聊天内容与凭据绝不输出。

## 示例

- `/cellulosesz permissions`
- `/cellulosesz permissions check cellulosesz.command.tp`

## 相关

- `/cellulosesz status`
- `/cellulosesz commands`
