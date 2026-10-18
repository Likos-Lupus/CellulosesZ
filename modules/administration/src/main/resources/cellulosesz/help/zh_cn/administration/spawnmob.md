# /spawnmob

生成一只或多只生物，可带坐骑。

## 行为

在你所在位置生成指定实体类型；也可指定目标，在其位置生成。数量受
`[administration.entityTools].maxSpawnAmount` 限制；被削减时反馈会同时给出请求数量与实际生成数量。

`entity` 支持 `mob[:data][,mount[:data]]`。存在坐骑时，每只生成的生物都会骑乘自己的坐骑，且需要
`cellulosesz.spawnmob.stack`。`:` 之后的实体数据在当前版本会被接受但忽略。

## 参数

- `entity`：实体类型 ID，可带 `:data` 和/或 `,mount`，例如 `minecraft:cow`、`zombie,chicken`。
- `amount`：可选，生成数量；缺省为 1。
- `player`：可选，以该玩家的位置生成；缺省为自己。

## 示例

- `/spawnmob cow`
- `/spawnmob zombie 10`
- `/spawnmob cow 5 Alice`
- `/spawnmob skeleton,spider`

## 失败情形

- 实体或坐骑类型未知。
- 目标不在线。
- 没有 `cellulosesz.spawnmob.stack` 却请求坐骑。

## 相关命令

- `/spawner`、`/remove`
