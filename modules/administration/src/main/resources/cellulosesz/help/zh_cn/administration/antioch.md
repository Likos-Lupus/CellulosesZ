# /antioch

在你看向的方块处生成点燃的 TNT。

## 行为

在所看的方块处生成点燃的 TNT。若提供 message，则向所有在线玩家广播经典的“安提阿圣手榴弹”两行台词；文本本身不会
作为物品名称。把 `[administration.fun].enabled` 设为 `false` 会停用整组娱乐命令。

## 参数

- `message`：可选；存在时触发广播。

## 示例

- `/antioch`
- `/antioch Hello`

## 失败情形

- 娱乐命令已停用。

## 相关命令

- `/beezooka`、`/kittycannon`
