# /lightning

召唤闪电。

## 行为

无目标时，在你看的方块处（最远 600 格）劈下真实闪电；有目标时，只显示闪电效果并造成可选伤害（默认 5），开启
god 的玩家不受伤害。power 是伤害，不是闪电数量。

## 参数

- `player`：可选目标。此分支需要 `cellulosesz.lightning.others`。
- `power`：可选伤害，1..100，默认 5；必须指定目标。

## 示例

- `/lightning`
- `/lightning Alice`
- `/lightning Alice 3`

## 失败情形

- 没有看向方块（无目标形式）。
- 目标不在线。

## 相关命令

- `/thunder`、`/nuke`、`/weather`
