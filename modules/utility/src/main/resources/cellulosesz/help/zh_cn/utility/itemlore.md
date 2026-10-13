# /itemlore

编辑手中物品的描述（lore）。

## 行为

替换手中物品的描述。用 `|` 分隔多行。不带文本运行时清除描述。服务器会限制行数（默认 10 行）和每行长度（默认
80 个 字符）。

## 示例

- `/itemlore Found in the deep mines`
- `/itemlore Line one|Line two`
- `/itemlore`

## 失败情形

- 你手上没有物品。
- 描述行数过多，或某一行过长。

## 相关命令

- `/itemname`
