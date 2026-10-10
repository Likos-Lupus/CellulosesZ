# /cellulosesz language

View or change your CellulosesZ language.

## Subcommands

- `/cellulosesz language`: show your current language.
- `/cellulosesz language list`: list the available languages.
- `/cellulosesz language server`: clear your choice and follow the server default.
- `/cellulosesz language <language>`: set your language (for example `zh_cn`).

## Behavior

Your choice is stored per player. CellulosesZ decides text language from your preference, then the
configured server default, then the `en_us` baseline. The client language is not used.

## Failures

- The language is not supported.
- Storing the preference failed.
