# /editsign

编辑你所看告示牌的文本。

## 行为

只编辑文本：输入中的点击与悬停事件一律被剥离。编辑的是你面向的一面；编辑背面需要
`[administration.signEditing].allowBack`。颜色代码仅在拥有相应权限时生效。`set` 写入一行，`clear` 清空
一行或整块告示牌，`copy` 将告示牌文本存入会话剪贴板，`paste` 再写回。剪贴板会在你离线时清除。

## 参数

- `set`：写入一行。
- `clear`：清空一行；不指定行时清空整块。
- `copy`：复制告示牌文本。
- `paste`：粘贴所复制的文本。
- `line`：行号，1 到 4。
- `text`：要写入的文本。

## 示例

- `/editsign set 1 Welcome`
- `/editsign clear 2`
- `/editsign copy`
- `/editsign paste`

## 失败情形

- 你没有看向告示牌。
- 告示牌编辑已停用、背面编辑被禁止，或距离过远。
- 文本超过配置的单行长度上限。

## 相关命令

- `/break`、`/gc`
