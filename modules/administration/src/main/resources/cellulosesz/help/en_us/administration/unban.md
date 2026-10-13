# /unban

Lift an account ban.

## Behavior

Removes the account from the vanilla ban list so the player can join again. The name must resolve to
a known account. Unbans are recorded in the audit log.

## Parameters

- `player`: the account to unban.

## Example

- `/unban Alice`

## Failures

- No account record exists for the name.
- The player is not banned.

## Related commands

- `/ban`, `/tempban`
