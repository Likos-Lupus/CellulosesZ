# /tempban

临时封禁一个玩家账号。

## 参数

- `player`：要封禁的玩家。
- `duration`：时长，例如 `1d`、`12h` 或 `1w`。
- `reason`：可选原因。

## 示例

`/tempban Alice 3d 破坏`

## 失败情形

- 时长非法或超过配置上限。
- 目标受保护。

## 相关命令

- `/ban`、`/unban`、`/tempbanip`
