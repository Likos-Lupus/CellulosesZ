# /sethome

Save your current position as a home.

## Behavior

With no name, saves or overwrites your default home (`home` by default). With a name, saves a named
home. The position is read where you run the command.

## Parameters

- `name`: optional home name. Lowercase letters, digits, `_` and `-`, up to 32 characters; defaults
  to your default home.

## Example

- `/sethome`
- `/sethome base`

## Failures

- The name is invalid.
- You have reached the home limit (10 by default).

## Related

- `/home`, `/delhome`, `/homes`
