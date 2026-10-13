# /cellulosesz

管理根命令。不带子命令时会显示模组版本、加载器与 Minecraft 版本。

## 子命令

- `/cellulosesz status`：显示 CellulosesZ 健康与存储状态（管理员）。
- `/cellulosesz reload`：重新加载配置（管理员）。
- `/cellulosesz language [list|server|<language>]`：查看或更改你的语言。

## 行为

`status` 显示当前存储后端、存储地址、权限后端与已记录的玩家数量。`reload` 会重新读取
`cellulosesz.toml`，但无法切换正在使用的数据库，更换后端需要重启服务器。

## 说明

重新加载若校验失败，会继续使用原来的配置。`status` 与 `reload` 需要管理员权限。

## 示例

- `/cellulosesz`
- `/cellulosesz status`

## 相关命令

- `/cellulosesz language`
