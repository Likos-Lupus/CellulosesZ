# /broadcastworld

Send an announcement to the players in one dimension.

## Behavior

The dimension is chosen first, then the message; only players in that dimension receive it. The text
is sent exactly as written. The default maximum length is 1,000 characters; the server can change
it.

## Parameters

- `dimension`: the dimension id, such as `minecraft:overworld`.
- `message`: the announcement text.

## Example

- `/broadcastworld minecraft:overworld mining event now`

## Failures

- The dimension id isn't valid.
- No players are in that dimension.
- Announcements are disabled.
- Missing permission.

## Related commands

- `/broadcast`
