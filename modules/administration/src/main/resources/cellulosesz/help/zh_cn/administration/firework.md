# /firework

编辑或发射烟花。

## 行为

第一种形式设置手持烟花火箭的飞行时间。`clear` 清空其爆炸效果但保留火箭。`fire` 从你的位置发射火箭；可选数量
受服务器上限限制。

## 参数

- `power`：手持火箭的飞行时间，受 `[administration.worldTools].maxFireworkPower` 限制。
- `clear`：移除手持火箭的所有爆炸效果。
- `fire`：发射火箭。
- `amount`：可选数量，受配置上限限制。

## 示例

- `/firework 2`
- `/firework clear`
- `/firework fire 3`

## 失败情形

- 你手上没有烟花火箭。

## 相关命令

- `/fireball`、`/nuke`
