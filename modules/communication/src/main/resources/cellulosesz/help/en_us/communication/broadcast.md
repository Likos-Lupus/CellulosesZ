# /broadcast

Send an announcement to every online player.

## Behavior

Announcements are shown with an "Announcement" label; the text is sent exactly as written. This
command needs the broadcast permission. The default maximum length is 1,000 characters; the server
can change it.

## Parameters

- `message`: the announcement text.

## Example

- `/broadcast server restart in 5 minutes`

## Failures

- The announcement is empty or too long.
- Announcements are disabled.
- Missing permission.

## Related commands

- `/broadcastworld`
