# /spawnmob

生成一只或多只生物。

## 行为

在你所在位置生成指定实体类型；也可指定目标，在其位置生成。数量受
`[administration.entityTools].maxSpawnAmount` 限制；被削减时反馈会同时给出请求数量与实际生成数量。

## 参数

- `entity`：实体类型 ID，例如 `minecraft:cow` 或 `cow`。
- `amount`：可选，生成数量；缺省为 1。
- `player`：可选，以该玩家的位置生成；缺省为自己。

## 示例

- `/spawnmob cow`
- `/spawnmob zombie 10`
- `/spawnmob cow 5 Alice`

## 失败情形

- 实体类型未知。
- 目标不在线。

## 相关命令

- `/spawner`、`/remove`
