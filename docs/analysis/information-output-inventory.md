# Information output inventory

Baseline: 2026-10-12 working tree (post command-DSL). This file tracks every player-visible text
path that must move to the semantic presentation pipeline. It is a living document: add a row when a
new output path is introduced.

## Ownership

- Domain services keep returning typed results; they never build display strings.
- Feature presenters map a typed result to a `LocalizedText`/`Notice`/`ListPage`/`DetailView`.
- `core.text.presentation` renders per recipient; `core.text.adventure.InformationOutput`/`Feedback`
  sends.
- `Messages.PREFIX`/`prefixed`/`renderPrefixedFor` were removed in IP-P5; `Messages.raw` remains
  only for surfaces that cannot use Adventure (see the exceptions ledger).

## Migration status

| Area                                                                                 | Module           | Primary sites                                                     | Status |
|--------------------------------------------------------------------------------------|------------------|-------------------------------------------------------------------|--------|
| Root command (`/cellulosesz`, status, reload, language)                              | application      | `RootCommand.kt`                                                  | done   |
| Command help documents                                                               | core/application | `CommandDocumentationService`, `AdventureDocumentRenderer`, pager | done   |
| Movement (home, warp, spawn, teleport, request)                                      | movement         | `MovementMessages` + 5 command files                              | done   |
| Communication (msg, reply, ignore, msgtoggle, mail, helpop, broadcast)               | communication    | `CommunicationMessages` + 5 command files                         | done   |
| Administration (kick/ban/mute/kill/gamemode/sudo/heal/feed/fly/god/socialspy/vanish) | administration   | `AdministrationMessages` + 10 command files                       | done   |
| Utility (kit, item, workstation, inspection)                                         | utility          | `UtilityMessages` + 4 command files                               | done   |

## Measured raw-output surface (start of IP-P0)

Counts from `modules/*/src/main`:

| Marker                                                     | Occurrences          |
|------------------------------------------------------------|----------------------|
| `Messages.prefixed` / `Messages.raw` / `renderPrefixedFor` | ~131 across 30 files |
| `Component.literal(`                                       | 8                    |
| `Component.text(`                                          | 19                   |
| `sendSystemMessage`                                        | 11                   |
| `sendSuccess(`                                             | 4                    |
| `sendFailure(`                                             | 3                    |
| `.disconnect(`                                             | 3                    |
| `joinToString(`                                            | 30                   |

Not every `Component.text`/`joinToString` is player-facing: command tokens, vanilla protocol
constants, item display names and logging are legitimate exceptions.

## Approved exceptions

See `information-output-exceptions.md`. A path is exempt only with a concrete reason (vanilla
protocol constant, user-authored body, logging, command token, test fixture). "Hard to migrate" is
not a reason.
