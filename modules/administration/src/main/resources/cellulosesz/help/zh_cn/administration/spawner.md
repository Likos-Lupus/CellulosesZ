# /spawner

修改你所看的刷怪笼。

## 行为

把你所看的刷怪笼改为生成新的实体类型。可选的延迟以游戏刻为单位，覆盖刷怪间隔，需要相应延迟权限。

## 参数

- `entity`：实体类型 ID，例如 `minecraft:zombie` 或 `zombie`。
- `delay`：可选，刷怪延迟游戏刻数，1 到 72000。

## 示例

- `/spawner zombie`
- `/spawner minecraft:skeleton 200`

## 失败情形

- 实体类型未知。
- 你没有看向刷怪笼方块。

## 相关命令

- `/spawnmob`、`/remove`
