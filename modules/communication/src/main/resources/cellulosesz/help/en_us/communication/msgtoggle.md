# /msgtoggle

View or change whether other players can send you private messages.

## Behavior

The setting is stored per player and keeps its value after a restart. Turning it off doesn't stop
mail, help requests, or announcements. With no argument, the current state is shown.

## Parameters

- `on`: allow private messages.
- `off`: block private messages.

## Example

- `/msgtoggle`
- `/msgtoggle off`

## Failures

- Message settings can't be read or saved right now.

## Related commands

- `/msg`, `/ignore`
