# /cellulosesz commands

针对当前调度器世代中 CellulosesZ 所拥有命令路由的只读诊断。

## 子命令

- `/cellulosesz commands`：输出简要摘要（世代、已安装与已跳过数量）。
- `/cellulosesz commands conflicts`：列出所有因已被占用而跳过的短名或别名，以及任何无法安装的必需路由。
- `/cellulosesz commands routes [name]`：列出 CellulosesZ 实际安装的路由，可按命令名或字面量过滤。

## 行为

归属来自最近一次注册结果，绝不猜测：被原版或其他模组先注册的短名会显示为 `skipped_already_present`；必需
路由（管理根命令或 `/cellulosesz:<command>` 入口）被意外占用时显示为 `install_mismatch`。短名被跳过时，
后备入口 `/cellulosesz:<command>` 仍然可用。

## 说明

只输出字面量与状态值；命令参数、玩家输入与权限信息绝不回显。仅限管理员使用。

## 示例

- `/cellulosesz commands`
- `/cellulosesz commands conflicts`
- `/cellulosesz commands routes home`

## 相关

- `/cellulosesz status`
