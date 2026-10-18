# /fireball

发射投掷物。

## 行为

朝你的视线方向发射一枚投掷物。默认类型是 `fireball`（大火球）；其他类型包括 `small`、`large`、`dragon`、
`skull`、`arrow`、`egg`、`snowball`、`expbottle`、`splashpotion`、`lingeringpotion`、`trident` 和
`windcharge`。速度默认为 2，并受 `[administration.worldTools].maxProjectileSpeed` 限制。

在末尾追加 `ride` 可以骑乘投掷物，需要 `cellulosesz.fireball.ride`。

## 参数

- `type`：可选，投掷物类型；缺省为 `fireball`。
- `speed`：可选，速度，0 到 64，受服务器上限限制。
- `ride`：可选，骑乘投掷物。

## 示例

- `/fireball`
- `/fireball small`
- `/fireball windcharge 2`
- `/fireball large 2.5 ride`

## 失败情形

- 类型无法识别。
- 你没有 `cellulosesz.fireball.<type>` 权限。
- 没有 `ride` 权限时会被拒绝。

## 相关命令

- `/firework`、`/nuke`、`/lightning`
