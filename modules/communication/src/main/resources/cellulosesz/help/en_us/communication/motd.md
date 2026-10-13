# /motd

Show the server message of the day.

## Usage

- `/motd`

## Behavior

The page is a Markdown file at `<config-dir>/cellulosesz/pages/<lang>/motd.md`, rendered in full in
your language. Editing the file takes effect the next time the page is viewed.

## Failures

- Server pages are disabled.
- The motd page hasn't been set up yet.
- The pages directory is missing, or the file is too large to display.

## Related commands

- `/rules`, `/info`, `/customtext`
