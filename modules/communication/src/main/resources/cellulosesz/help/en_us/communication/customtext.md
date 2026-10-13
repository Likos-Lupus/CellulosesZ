# /customtext

Show a named custom server page.

## Usage

- `/customtext <page>`

## Behavior

`<page>` is the file name without `.md` under `<config-dir>/cellulosesz/pages/<lang>/`. The page is
rendered in full in your language; if it isn't available in your language, the `en_us` file is used.
Editing the file takes effect the next time the page is viewed.

## Failures

- No page with that name exists.
- The page name isn't valid; use letters, digits, `_` and `-`.
- Server pages are disabled.
- The pages directory is missing, or the file is too large to display.

## Related commands

- `/motd`, `/rules`, `/info`
