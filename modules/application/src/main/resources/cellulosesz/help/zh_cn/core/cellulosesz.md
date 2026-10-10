# /cellulosesz

管理根命令。

## 子命令

- `status`：显示 CellulosesZ 健康与存储状态（管理员）。
- `reload`：重新加载配置（管理员）。
- `language [list|server|<language>]`：查看或更改你的语言。

## 说明

`reload` 以事务方式重新读取 TOML；若活动数据库连接设置发生变化会被拒绝，需重启服务器。

## 相关命令

- `/cellulosesz language`
