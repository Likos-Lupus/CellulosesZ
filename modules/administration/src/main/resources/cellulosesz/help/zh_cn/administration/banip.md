# /banip

封禁一个 IP 地址。

## 参数

- `target`：IP 字面量，或在线玩家名（使用其地址）。
- `reason`：可选原因。

## 示例

`/banip 203.0.113.7`
`/banip Alice`

## 行为

使用原版 IP 封禁列表。原版仅能完整执行 IPv4 限制，详见管理 ADR。

## 失败情形

- 地址无法解析。
- 指定在线玩家受保护。
- 缺少权限。

## 相关命令

- `/tempbanip`、`/unbanip`、`/ban`
