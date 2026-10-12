# Information output exceptions

Paths that intentionally keep raw vanilla `Component`/text output, with a reason. Reviewed per
change; a new entry must name a concrete reason.

| Path                                                                      | Reason                                                                                                                                               | Owner          |
|---------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|
| `MinecraftBanBackend` disconnect reason                                   | Connects to the vanilla disconnect channel; the mod owns the wording but must hand it to `disconnect(Component)`                                     | administration |
| `MinecraftInspectionBackend` container titles                             | Vanilla menu titles are vanilla `Component`s                                                                                                         | utility        |
| Item display names (`ItemStack.hoverName`)                                | Vanilla client-translated text must not be re-translated                                                                                             | utility        |
| `Logger`/audit records                                                    | Technical records, never player-facing                                                                                                               | all            |
| Command literals / permission nodes / paths                               | Not localized                                                                                                                                        | all            |
| Test fixtures                                                             | Not production                                                                                                                                       | all            |
| `CommandDsl.executesPlayer` "requires a player" fallback (`Messages.raw`) | The DSL build step has no language context; this is a console-only fallback. Modern commands use `SourceAccess.PLAYER` or localized notices instead. | minecraft-core |
