# /deljail

Delete a jail location.

## Behavior

Removes the named jail. A jail that still holds an active sentence cannot be deleted; release those
players first, then delete it.

## Parameters

- `name`: the jail to delete.

## Example

- `/deljail cells`

## Failures

- No jail has that name.
- The jail still has active sentences.
- Jail records can't be read or saved right now.

## Related commands

- `/setjail`, `/jails`, `/jailedplayers`
