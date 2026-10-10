# /mail

Offline-capable mail.

## Subcommands

- `/mail`: show a summary (unread count).
- `/mail read [page]`: read messages, ten per page.
- `/mail send <player> <message>`: send mail.
- `/mail sendtemp <player> <duration> <message>`: send mail that expires.
- `/mail clear`: delete all of your mail.

## Parameters

- `page`: optional page number, starting at 1.
- `duration`: for `sendtemp`, a duration such as `1d`, `12h` or `1d12h`.

## Example

`/mail send Alice see you tomorrow`
`/mail sendtemp Alice 2d remember to water the crops`
`/mail read 2`

## Failures

- You are sending to an unknown player.
- The duration is invalid.
- The mailbox is full or the message is empty.

## Related

- `/msg`
