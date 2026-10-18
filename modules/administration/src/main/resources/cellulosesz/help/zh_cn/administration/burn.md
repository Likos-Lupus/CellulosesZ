# /burn

点燃目标。

## 行为

让目标燃烧指定的秒数。时长受 `[administration.worldTools].maxBurnSeconds` 限制。

## 参数

- `player`：目标。
- `seconds`：燃烧时长，从 0.1 到配置上限。

## 示例

- `/burn Alice 5`
- `/burn Bob 2.5`

## 失败情形

- 目标不在线。

## 相关命令

- `/ext`、`/nuke`、`/lightning`
