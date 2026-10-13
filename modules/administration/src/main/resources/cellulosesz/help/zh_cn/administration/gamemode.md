# /gamemode

更改玩家的游戏模式。

## 行为

设置指定玩家的游戏模式；不填玩家时以自己为目标。模式必须是 `survival`、`creative`、`adventure` 或
`spectator`
之一。受保护的目标无法更改，除非目标是你自己。该操作会记入审计记录。

## 参数

- `mode`：`survival`、`creative`、`adventure` 或 `spectator`。
- `player`：可选目标，缺省为自己。

## 示例

- `/gamemode creative`
- `/gamemode survival Alice`

## 失败情形

- 模式未知。
- 目标不在线。
- 目标受保护。

## 相关命令

- `/fly`、`/god`
