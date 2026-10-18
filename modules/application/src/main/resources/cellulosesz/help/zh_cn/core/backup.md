# /backup

立即执行一次配置好的外部备份命令。

## 用法

- `/backup`：立即开始备份。

## 行为

- 命令从 `[backup].command` 读取为参数列表并直接启动， **不经过 shell**。
- 同一时间只允许一个备份运行；重复请求会提示已有备份正在运行。
- 进程在服务器线程之外运行，并受 `timeoutSeconds` 硬超时约束；超时会被强制终止。
- 返回退出码；捕获的输出写入服务器日志。
- 当 `intervalMinutes` 大于 0 时，同一命令还会按该间隔定时执行。除非设置 `alwaysRun`，无人在线时会跳过定时
  备份。

## 权限

`cellulosesz.command.backup`（默认管理员）。
