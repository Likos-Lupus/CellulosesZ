# /cellulosesz

Administrative root command.

## Subcommands

- `status`: show CellulosesZ health and storage status (moderator).
- `reload`: reload the configuration (moderator).
- `language [list|server|<language>]`: view or change your language.

## Notes

`reload` re-reads the TOML file transactionally; a change to the active database connection settings
is rejected and requires a server restart.

## Related

- `/cellulosesz language`
