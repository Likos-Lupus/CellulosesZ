# /cellulosesz language

查看或更改你的 CellulosesZ 语言。

## 子命令

- `/cellulosesz language`：显示当前语言。
- `/cellulosesz language list`：列出可用语言。
- `/cellulosesz language server`：清除个人选择，跟随服务器默认。
- `/cellulosesz language <language>`：设置语言（例如 `zh_cn`）。

## 行为

你的选择按玩家存储。CellulosesZ 依次按个人偏好、服务器默认、`en_us` 基线决定文本语言，不使用客户端语言。

## 失败情形

- 该语言不受支持。
- 偏好存储失败。
