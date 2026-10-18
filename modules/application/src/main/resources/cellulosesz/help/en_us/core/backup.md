# /backup

Runs the server's configured external backup command once.

## Usage

- `/backup`: start a backup now.

## Behavior

- The command is read from `[backup].command` as an argument vector and started directly; it is
  never passed through a shell.
- Only one backup may run at a time. A second request reports that a backup is already running.
- The process runs off the server thread with a hard timeout (`timeoutSeconds`); on timeout it is
  forcibly stopped.
- The exit code is reported. Captured output is written to the server log.
- When `intervalMinutes` is greater than zero, the same command also runs on that schedule. A
  scheduled run is skipped when no player is online unless `alwaysRun` is set.

## Permission

`cellulosesz.command.backup` (administrator by default).
