# /settpr

创建和管理随机传送区域。

## 行为

需要管理员权限。区域是一个位于单一维度、围绕中心点的圆环；`/tpr` 会在最小半径与最大半径之间按面积均匀取点。
中心取自执行 `create` 或 `center` 时的位置。每次修改中心或半径都会递增区域版本号，从而取消仍在按旧形状进行的搜索。

- `/settpr list` 列出所有区域及其所在维度。
- `/settpr info <region>` 显示某个区域的维度、中心、半径范围和状态。
- `/settpr create <region> <minRadius> <maxRadius>` 以你当前位置为中心创建或替换区域。
- `/settpr center <region>` 将区域中心移动到你的位置，仅限同一维度。
- `/settpr range <region> <minRadius> <maxRadius>` 修改区域的半径。
- `/settpr remove <region>` 删除区域。

## 参数

- `region`：区域名称。小写字母、数字、`_` 和 `-`，最长 32 个字符。
- `minRadius`：距中心最近的距离（格）。必须 `>= 0`。
- `maxRadius`：距中心最远的距离（格）。必须大于 `minRadius`，且不超过服务器的随机传送上限。

## 示例

- `/settpr create wild 500 2000`
- `/settpr range wild 100 1500`
- `/settpr center wild`
- `/settpr info wild`
- `/settpr list`
- `/settpr remove wild`

## 失败情形

- 名称非法。
- 半径超出允许范围。
- `center` 指定的区域位于其他维度。
- 区域数量已达到上限。
- 找不到该区域。
- 缺少权限。

## 相关命令

- `/tpr`
