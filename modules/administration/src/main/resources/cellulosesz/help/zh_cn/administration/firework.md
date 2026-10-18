# /firework

编辑或发射烟花。

## 行为

第一种形式设置手持烟花火箭的飞行时间。`clear` 清空其爆炸效果但保留火箭。`fire` 从你的位置发射火箭。

`fire` 接受整数数量或方向词。整数会向上发射对应数量，受 `[administration.worldTools].maxFireworkAmount` 
限制；非整数（例如 `north`）会朝你所看方向发射一枚，飞行时间限制为 1。`fire` 需要 
`cellulosesz.firework.fire`。

## 参数

- `power`：手持火箭的飞行时间，受 `[administration.worldTools].maxFireworkPower` 限制。
- `clear`：移除手持火箭的所有爆炸效果。
- `fire`：发射火箭。
- `amountOrDirection`：可选，整数数量或方向词。

## 示例

- `/firework 2`
- `/firework clear`
- `/firework fire 3`
- `/firework fire north`

## 失败情形

- 你手上没有烟花火箭。
- 没有 `cellulosesz.firework.fire` 时 `fire` 会被拒绝。

## 相关命令

- `/fireball`、`/nuke`
