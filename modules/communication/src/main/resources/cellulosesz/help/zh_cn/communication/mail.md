# /mail

支持离线收发的邮件。

## 子命令

- `/mail`：查看概览（未读数）。
- `/mail read [page]`：阅读邮件，每页十条。
- `/mail send <player> <message>`：发送邮件。
- `/mail sendtemp <player> <duration> <message>`：发送会过期的邮件。
- `/mail clear`：清空你的全部邮件。

## 参数

- `page`：可选页码，从 1 开始。
- `duration`：`sendtemp` 的时长，例如 `1d`、`12h` 或 `1d12h`。

## 示例

`/mail send Alice 明天见`
`/mail sendtemp Alice 2d 记得浇庄稼`
`/mail read 2`

## 失败情形

- 收件人未知。
- 时长非法。
- 邮箱已满或消息为空。

## 相关命令

- `/msg`
