# /powertool

将命令绑定到物品的点击。

## 参数

- `<command>`：把命令绑定到左键和右键。
- `l:<command>`：绑定到左键。
- `r:<command>`：绑定到右键。
- `c:<command>`：绑定到左键和右键。
- 无参数时移除手中物品的绑定。

## 行为

用绑定的物品点击时以你自己的身份运行该命令。强力工具不能调用另一个强力工具命令，每位玩家每秒最多触发一次。匹配
到强力工具时会取消该交互。

## 示例

- `/powertool spawn`
- `/powertool l:home`
- `/powertool r:workbench`

## 失败情形

- 手上没有物品。
- 命令会再次进入强力工具。
- 强力工具已关闭。

## 相关命令

- `/powertoollist`、`/powertooltoggle`
