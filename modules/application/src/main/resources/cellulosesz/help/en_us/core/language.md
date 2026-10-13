# /cellulosesz language

View or change your CellulosesZ language.

## Subcommands

- `/cellulosesz language`: show your current language.
- `/cellulosesz language list`: list the available languages.
- `/cellulosesz language server`: clear your choice and follow the server default.
- `/cellulosesz language <language>`: set your language, for example `zh_cn` or `en_us`.

## Behavior

Your choice is saved per player. CellulosesZ picks your language from your preference, then the
server default, then the `en_us` baseline. The client language is not used.

## Example

- `/cellulosesz language`
- `/cellulosesz language list`
- `/cellulosesz language server`
- `/cellulosesz language zh_cn`

## Failures

- The language is not supported. Run `/cellulosesz language list` to see the choices.
- Saving the preference failed; try again later.

## Related

- `/cellulosesz`
