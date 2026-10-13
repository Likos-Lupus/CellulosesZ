# /tpauto

自动接受传送请求，并选择哪些玩家可以不经过询问。

## 行为

自动接受只针对方向为“传送到你身边”的请求（其他玩家的 `/tpa`），请求者会被传送到你身边。`/tpahere` 请求永远不会
被自动接受，因为那会把你传送到请求者身边。

共有三种模式：

- `off`：不自动接受任何请求。
- `trusted`：只自动接受信任名单中玩家的请求。
- `all`：自动接受所有符合条件的请求者。忽略与可见性规则仍然生效。

- `/tpauto` 显示当前模式和可用子命令。
- `/tpauto off`、`/tpauto trusted`、`/tpauto all` 设置模式。
- `/tpauto allow <player>` 把玩家加入信任名单。
- `/tpauto deny <player>` 把玩家移出信任名单。
- `/tpauto list` 显示信任名单。

## 示例

- `/tpauto trusted`
- `/tpauto allow Alice`
- `/tpauto list`
- `/tpauto off`

## 限制与注意

- `trusted` 模式下名单为空时不会自动接受任何人。
- 名单容量来自 `[movement.requests].maxAutoAllowedPlayers`，可由服务器修改。
- 自动接受只针对 `/tpa`（请求者传送到你身边）。`/tpahere` 仍会询问你。
- 名单中的玩家只是免去 `/tpa` 的询问，不获得其他权限。

## 失败情形

- 指定的玩家不在线。
- 该玩家已在名单中。
- 名单已满。
- 暂时无法读取或保存自动接受设置。

## 相关命令

- `/tpa`、`/tpahere`、`/tpaccept`、`/tpdeny`、`/tpaall`
