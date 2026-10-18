# /book

编辑或转换手中的书。

## 参数

- 无参数时在成书与书与笔之间转换。
- `author <name>`：设置成书的作者。
- `title <title>`：设置成书的标题。

## 行为

作用于主手中的书。作者与标题需要成书；转换会保留书页内容。

## 示例

- `/book`
- `/book author Notch`
- `/book title My Story`

## 失败情形

- 手上没有物品或手中物品不是书。
- 对书与笔使用 author/title。

## 相关命令

- `/itemname`、`/itemlore`
