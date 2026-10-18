package top.likoslupus.cellulosesz.application.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.dsl.CommandSyntax
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.registry.CommandRouteStatus
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionBackendStatus
import top.likoslupus.cellulosesz.core.permission.PermissionInspector
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages

/** `/cellulosesz commands` and `/cellulosesz permissions` with their read-only sub-trees. */
internal fun CommandSyntax.diagnosticsCommands(
    diagnostics: CommandRegistrationDiagnostics,
    permissions: PermissionService,
    inspector: PermissionInspector?,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
) {
    literal(
        name = "commands",
        permission = CommandPermissions.ROOT_COMMANDS,
        documentation = "core/commands",
    ) {
        executes {
            showCommandSummary(
                source,
                diagnostics,
                messages,
                adventure
            )
        }
        literal("conflicts") {
            executes {
                showCommandConflicts(
                    source,
                    diagnostics,
                    messages,
                    adventure
                )
            }
        }
        literal("routes") {
            executes {
                showCommandRoutes(
                    source,
                    null,
                    diagnostics,
                    messages,
                    adventure
                )
            }
            argument("name", word()) { name ->
                executes {
                    showCommandRoutes(
                        source,
                        get(name),
                        diagnostics,
                        messages,
                        adventure
                    )
                }
            }
        }
    }
    literal(
        name = "permissions",
        permission = CommandPermissions.ROOT_PERMISSIONS,
        documentation = "core/permissions",
    ) {
        executes {
            showPermissionsStatus(
                source,
                permissions,
                inspector,
                messages,
                adventure,
            )
        }
        literal("status") {
            executes {
                showPermissionsStatus(
                    source,
                    permissions,
                    inspector,
                    messages,
                    adventure,
                )
            }
        }
        literal("check") {
            argument("node", word()) { node ->
                executes {
                    checkPermission(
                        source,
                        get(node),
                        permissions,
                        inspector,
                        messages,
                        adventure,
                    )
                }
            }
        }
    }
}

private fun showCommandSummary(
    source: CommandSourceStack,
    diagnostics: CommandRegistrationDiagnostics,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int {
    val playerId = source.player?.uuid
    val report = diagnostics.snapshot()
        ?: return source.reply(
            adventure,
            messages.renderFor(playerId, MessageKey("core.commands.not_ready"))
        )
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.commands.routes_title"),
            listOf(MessageArgument(report.generation.toString()))
        ),
    )
    return source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.commands.summary"),
            listOf(
                MessageArgument(report.installedCount.toString()),
                MessageArgument(report.skippedCount.toString()),
            )
        ),
    )
}

private fun showCommandConflicts(
    source: CommandSourceStack,
    diagnostics: CommandRegistrationDiagnostics,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int {
    val playerId = source.player?.uuid
    val report = diagnostics.snapshot()
        ?: return source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.commands.not_ready")
            )
        )
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.commands.conflicts_title")
        )
    )
    if (report.conflicts.isEmpty()) {
        return source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.commands.conflicts_none")
            )
        )
    }
    report.conflicts.forEach { conflict ->
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.commands.conflict"),
                listOf(
                    MessageArgument(conflict.literal),
                    MessageArgument(conflict.kind.name.lowercase()),
                    MessageArgument(conflict.status.name.lowercase()),
                    MessageArgument(conflict.fallback ?: "-"),
                ),
            ),
        )
    }
    return report.conflicts.size
}

private fun showCommandRoutes(
    source: CommandSourceStack,
    filter: String?,
    diagnostics: CommandRegistrationDiagnostics,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int {
    val playerId = source.player?.uuid
    val report = diagnostics.snapshot()
        ?: return source.reply(
            adventure,
            messages.renderFor(playerId, MessageKey("core.commands.not_ready"))
        )
    val routes = report.results
            .filter { it.status == CommandRouteStatus.INSTALLED }
            .filter { filter == null || it.commandName == filter || it.literal == filter }

    if (filter == null) {
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.commands.routes_title"),
                listOf(MessageArgument(report.generation.toString()))
            ),
        )
    } else {
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.commands.routes_filtered"),
                listOf(MessageArgument(filter))
            ),
        )
    }

    if (routes.isEmpty()) {
        return source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.commands.routes_none"),
                listOf(MessageArgument(filter ?: "*"))
            ),
        )
    }
    routes.forEach { route ->
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.commands.route"),
                listOf(
                    MessageArgument(route.literal),
                    MessageArgument(route.kind.name.lowercase()),
                ),
            ),
        )
    }
    return routes.size
}

private fun showPermissionsStatus(
    source: CommandSourceStack,
    permissions: PermissionService,
    inspector: PermissionInspector?,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int {
    val playerId = source.player?.uuid
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.permissions.backend"),
            listOf(MessageArgument(permissions.bridge.backendName)),
        ),
    )
    val native = when {
        inspector == null -> MessageKey("core.permissions.native_absent")
        else -> MessageKey("core.permissions.native_present")
    }
    return source.reply(adventure, messages.renderFor(playerId, native))
}

private fun checkPermission(
    source: CommandSourceStack,
    rawNode: String,
    permissions: PermissionService,
    inspector: PermissionInspector?,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int {
    val playerId = source.player?.uuid
    val node = rawNode.trim().removePrefix("/")
    val spec = CommandPermissions.all.firstOrNull { it.node == node }
        ?: return source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.permissions.unknown"),
                listOf(MessageArgument(node)),
            ),
        )
    val effective = permissions.allows(source, spec)
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.permissions.effective"),
            listOf(
                MessageArgument(spec.node),
                MessageArgument(effective.toString()),
            ),
        ),
    )
    val inspection = inspector?.inspect(source, spec) ?: return 1
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.permissions.native_opinion"),
            listOf(
                MessageArgument(inspection.status.name.lowercase()),
                MessageArgument(inspection.luckPermsOpinion.name.lowercase()),
            ),
        ),
    )
    if (inspection.status == PermissionBackendStatus.LUCKPERMS_ACTIVE &&
        inspection.contextSummary.isNotEmpty()
    ) {
        val context = inspection.contextSummary.entries.joinToString(", ") { (key, values) ->
            "$key=${values.joinToString("|")}"
        }
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.permissions.context"),
                listOf(MessageArgument(context)),
            ),
        )
    }
    return 1
}
