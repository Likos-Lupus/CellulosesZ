# /renamehome

Rename one of your homes.

## Behavior

The home keeps its saved location. Renaming fails if the old name doesn't exist or the new name is
already taken.

## Parameters

- `old`: the current home name.
- `new`: the new home name. Lowercase letters, digits, `_` and `-`, up to 32 characters.

## Example

- `/renamehome home base`

## Failures

- The old name doesn't exist.
- The new name is already used.
- A name is invalid.

## Related

- `/home`, `/homes`, `/delhome`, `/sethome`
