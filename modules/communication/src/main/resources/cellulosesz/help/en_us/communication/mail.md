# /mail

Send and read mail that is stored for offline players.

## Subcommands

- `/mail`: show how many messages you have and how many are unread.
- `/mail read [page]`: open your inbox, ten messages per page.
- `/mail send <player> <message>`: send mail.
- `/mail sendtemp <player> <duration> <message>`: send mail that expires.
- `/mail clear`: delete all of your mail. This can't be undone.

## Behavior

Mail is kept on the server, so the recipient can read it while offline. Opening a page marks the
messages on it as read. The default mailbox capacity is 100 messages and the default send limit is
10 per minute; the server can change both. Mail body length defaults to 1,000 characters. A
temporary message is deleted once its expiry passes.

## Parameters

- `page`: page number, starting at 1.
- `duration`: for `sendtemp`, such as `1d`, `12h` or `1d12h`.

## Example

- `/mail read`
- `/mail send Alice see you tomorrow`
- `/mail sendtemp Alice 2d remember to water the crops`
- `/mail read 2`
- `/mail clear`

## Failures

- No record exists for the recipient.
- You're sending too quickly, or the recipient's mailbox is full.
- The duration is invalid or longer than the server allows.
- The message is empty or too long.

## Related commands

- `/msg`
