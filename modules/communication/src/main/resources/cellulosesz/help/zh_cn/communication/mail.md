# /mail

收发保存在服务器上的邮件，离线玩家也能收到。

## 子命令

- `/mail`：查看邮件总数和未读数。
- `/mail read [page]`：打开收件箱，每页十条。
- `/mail send <player> <message>`：发送邮件。
- `/mail sendtemp <player> <duration> <message>`：发送会过期的邮件。
- `/mail clear`：清空你的全部邮件，此操作无法撤销。

## 行为

邮件保存在服务器上，收件人离线也能阅读。打开某一页会将该页标记为已读。默认邮箱容量为 100 封， 默认发送频率为每分钟
10 封，二者都可由服务器调整；邮件正文默认上限为 1000 个字符。临时邮件在过期后被删除。

## 参数

- `page`：页码，从 1 开始。
- `duration`：`sendtemp` 的时长，例如 `1d`、`12h` 或 `1d12h`。

## 示例

- `/mail read`
- `/mail send Alice 明天见`
- `/mail sendtemp Alice 2d 记得浇庄稼`
- `/mail read 2`
- `/mail clear`

## 失败情形

- 找不到收件人的玩家记录。
- 发送过于频繁，或收件人邮箱已满。
- 时长不合法，或超过服务器允许的上限。
- 消息为空或过长。

## 相关命令

- `/msg`
