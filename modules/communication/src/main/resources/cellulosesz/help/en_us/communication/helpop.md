# /helpop

Send a request to the online staff.

## Behavior

The request is delivered to every online staff member and recorded in the server log, so it is kept
even when no one is online to read it. It is rate limited: the default is 6 requests per minute.
This channel stays available while you are muted.

## Parameters

- `message`: the request text.

## Example

- `/helpop someone is griefing spawn`

## Failures

- The request is empty or too long.
- You're sending requests too quickly.
- Staff help requests are disabled.

## Related commands

- `/msg`, `/mail`
