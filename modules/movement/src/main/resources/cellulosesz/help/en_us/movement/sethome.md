# /sethome

Save your current position as a home.

## Behavior

With no name, saves or overwrites your default home. With a name, saves a named home. The position
is captured on the server thread.

## Parameters

- `name`: optional home name. Lowercase letters, digits, `_` and `-`, up to 32 characters. Defaults
  to your default home.

## Example

`sethome`
`sethome base`

## Failures

- The name is invalid.
- You have reached the configured home limit.

## Related

- `/home`, `/delhome`, `/homes`
