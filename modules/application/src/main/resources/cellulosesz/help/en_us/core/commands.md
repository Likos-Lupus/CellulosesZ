# /cellulosesz commands

Read-only diagnostics for the command routes CellulosesZ owns in the current dispatcher generation.

## Subcommands

- `/cellulosesz commands`: print a compact summary (generation, installed and skipped counts).
- `/cellulosesz commands conflicts`: list every short name or alias that was skipped because it was
  already taken, plus any required route that could not be installed.
- `/cellulosesz commands routes [name]`: list the routes CellulosesZ actually installed, optionally
  filtered by command name or literal.

## Behavior

Ownership is reported from the last registration pass, never guessed: a short name that was already
registered by vanilla or another mod is shown as `skipped_already_present`, and a required route
(the admin root or a `/cellulosesz:<command>` entry) that was unexpectedly occupied is shown as
`install_mismatch`. When a short name is skipped, the namespaced fallback `/cellulosesz:<command>`
stays available.

## Notes

Only literals and status values are printed; command arguments, player input and permissions are
never echoed. Moderators only.

## Example

- `/cellulosesz commands`
- `/cellulosesz commands conflicts`
- `/cellulosesz commands routes home`

## Related

- `/cellulosesz status`
