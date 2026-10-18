# /cellulosesz help

列出你有权使用的 CellulosesZ 命令。

## 用法

- `/cellulosesz help [页码]`：分页列出命令，每页十条。
- `/help <命令>`：显示单个 CellulosesZ 命令的完整文档（vanilla 帮助覆盖层）。单个命令的文档不分页。

## 行为

- 列表按你的权限过滤，并排除配置中禁用的命令。
- 每条显示命令名和简短说明。
- 除非 `[commands.vanillaOverrides]` 显式接管，vanilla `/help` 保持不变。
