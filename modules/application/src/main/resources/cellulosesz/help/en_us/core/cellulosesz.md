# /cellulosesz

Administrative root command. With no subcommand it prints the mod version, the loader and the
Minecraft version.

## Subcommands

- `/cellulosesz status`: show CellulosesZ health and storage status (moderator).
- `/cellulosesz reload`: reload the configuration (moderator).
- `/cellulosesz language [list|server|<language>]`: view or change your language.

## Behavior

`status` shows the current storage backend, its address, the permission backend and the number of
stored player records. `reload` re-reads `cellulosesz.toml`; it cannot switch the active database,
so moving to a different backend needs a server restart.

## Notes

A reload that fails validation keeps the previous configuration in use. `status` and `reload` need
moderator permission.

## Example

- `/cellulosesz`
- `/cellulosesz status`

## Related

- `/cellulosesz language`
