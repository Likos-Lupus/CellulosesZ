# /cellulosesz help

Lists the CellulosesZ commands you are allowed to use.

## Usage

- `/cellulosesz help [page]`: list commands, ten per page.
- `/help <command>`: show the full documentation for one CellulosesZ command (the vanilla help
  overlay). Single-command documentation is never split across pages.

## Behavior

- The list is filtered by your permissions and excludes commands disabled in the configuration.
- Each entry shows the command name and its short description.
- Vanilla `/help` is left untouched unless `[commands.vanillaOverrides]` explicitly takes it over.
