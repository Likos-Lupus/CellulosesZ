package top.likoslupus.cellulosesz.application.command

import kotlinx.coroutines.CancellationException
import net.minecraft.SharedConstants
import net.minecraft.commands.CommandSourceStack
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.command.registry.CommandRouteStatus
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.message
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.TranslationCatalog
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.model.DetailField
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import top.likoslupus.cellulosesz.foundation.config.ConfigStore
import java.util.*

internal object RootCommand {

    const val ROOT_LITERAL: String = "cellulosesz"

    private val LOGGER = LoggerFactory.getLogger(RootCommand::class.java)

    fun definition(
        config: ConfigStore<CellulosesConfig>,
        kernel: RuntimeKernel,
        health: ApplicationHealth,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        languages: PlayerLanguagePreferences,
        resolver: LanguageResolver,
        catalog: TranslationCatalog,
        modVersion: String,
        loaderName: String,
        diagnostics: CommandRegistrationDiagnostics,
        onReloadApplied: suspend () -> Unit = {},
    ): CommandDefinition =
        command(
            name = ROOT_LITERAL,
            category = CommandCategory.ROOT,
            permission = CommandPermissions.ROOT,
            documentation = "core/cellulosesz",
        ) {
            executes {
                showInfo(
                    source,
                    modVersion,
                    loaderName,
                    messages,
                    adventure
                )
            }
            literal("status", permission = CommandPermissions.ROOT_STATUS) {
                executes {
                    showStatus(
                        source,
                        health,
                        presentation,
                        adventure
                    )
                }
            }
            literal("reload", permission = CommandPermissions.ROOT_RELOAD) {
                executes {
                    reload(
                        source,
                        config,
                        kernel,
                        messages,
                        adventure,
                        onReloadApplied
                    )
                }
            }
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
                name = "language",
                permission = CommandPermissions.ROOT_LANGUAGE,
                documentation = "core/language",
            ) {
                executes {
                    showLanguage(
                        source,
                        messages,
                        adventure,
                        languages,
                        resolver
                    )
                }
                literal("list") {
                    executes {
                        listLanguages(
                            source,
                            messages,
                            adventure,
                            catalog
                        )
                    }
                }
                literal("server") {
                    executesPlayer {
                        resetLanguage(
                            source,
                            kernel,
                            messages,
                            adventure,
                            languages,
                            resolver
                        )
                    }
                }
                argument("language", word()) { language ->
                    suggests { catalog.languages.forEach { suggest(it.value) } }
                    executesPlayer {
                        setLanguage(
                            source = source,
                            raw = get(language),
                            kernel = kernel,
                            messages = messages,
                            adventure = adventure,
                            languages = languages,
                            catalog = catalog,
                        )
                    }
                }
            }
        }

    private fun showInfo(
        source: CommandSourceStack,
        modVersion: String,
        loaderName: String,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): Int {
        val playerId = source.player?.uuid
        val minecraftVersion = SharedConstants.getCurrentVersion().name()
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.info.version"),
                listOf(MessageArgument(modVersion))
            )
        )
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.info.loader"),
                listOf(MessageArgument(loaderName))
            )
        )
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.info.minecraft"),
                listOf(MessageArgument(minecraftVersion))
            )
        )
        return source.reply(
            adventure,
            messages.renderFor(playerId, MessageKey("core.info.hint"))
        )
    }

    private fun showStatus(
        source: CommandSourceStack,
        health: ApplicationHealth,
        presentation: TextPresentation,
        adventure: AdventureRuntime,
    ): Int {
        val detail = DetailView(
            title = LocalizedText(MessageKey("core.status.title")),
            fields = listOf(
                field(MessageKey("core.status.state"), health.state.name),
                field(MessageKey("core.status.loader"), health.loader),
                field(MessageKey("core.status.permissions"), health.permissionBackend),
                field(MessageKey("core.status.storage"), health.storageType ?: "-"),
                field(MessageKey("core.status.endpoint"), health.storageEndpoint ?: "-"),
                field(MessageKey("core.status.identity"), health.identityRecords.toString()),
                field(MessageKey("core.status.migration"), health.migration),
            ),
        )
        val lines = presentation.renderFor(source.player?.uuid, detail)
        lines.forEach { source.reply(adventure, it) }
        return if (lines.isEmpty()) 0 else 1
    }

    private fun field(label: MessageKey, value: String): DetailField =
        DetailField(
            label = LocalizedText(label),
            value = LocalizedText(
                MessageKey("core.status.value"),
                listOf(TextArgument.Literal(value))
            ),
        )

    private fun showLanguage(
        source: CommandSourceStack,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        languages: PlayerLanguagePreferences,
        resolver: LanguageResolver,
    ): Int {
        val playerId = source.player?.uuid
        val explicit = playerId?.let(languages::explicit)
        val effective = resolver.effective(playerId)
        val key = when {
            explicit != null -> MessageKey("core.language.current")
            else -> MessageKey("core.language.current_inherited")
        }
        return source.reply(
            adventure,
            messages.renderFor(
                playerId,
                key,
                listOf(MessageArgument(effective.value))
            )
        )
    }

    private fun listLanguages(
        source: CommandSourceStack,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        catalog: TranslationCatalog,
    ): Int {
        val listed = catalog.languages.joinToString(", ") { it.value }
        return source.reply(
            adventure,
            messages.renderFor(
                source.player?.uuid,
                MessageKey("core.language.available"),
                listOf(MessageArgument(listed))
            ),
        )
    }

    private fun setLanguage(
        source: CommandSourceStack,
        raw: String,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        languages: PlayerLanguagePreferences,
        catalog: TranslationCatalog,
    ): Int {
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )

        val requested = LanguageId.parse(raw)
        if (requested == null || !catalog.supports(requested)) {
            return source.reply(
                adventure,
                messages.renderFor(
                    player.uuid,
                    MessageKey("core.language.unknown"),
                    listOf(MessageArgument(raw))
                ),
            )
        }

        val target = source.feedbackTarget()
        val playerId = player.uuid
        val job = kernel.launch {
            try {
                languages.set(playerId, requested)
                kernel.message(
                    target,
                    adventure,
                    messages.renderFor(
                        playerId,
                        MessageKey("core.language.changed"),
                        listOf(MessageArgument(requested.value))
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LOGGER.warn("unable to persist language preference for {}", playerId, error)
                kernel.message(
                    target,
                    adventure,
                    messages.renderFor(
                        playerId,
                        MessageKey("core.language.storage_failed")
                    )
                )
            }
        }
        return acknowledge(
            source,
            adventure,
            messages,
            playerId,
            job == null
        )
    }

    private fun resetLanguage(
        source: CommandSourceStack,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        languages: PlayerLanguagePreferences,
        resolver: LanguageResolver,
    ): Int {
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )

        val target = source.feedbackTarget()
        val playerId = player.uuid
        val job = kernel.launch {
            try {
                languages.reset(playerId)
                kernel.message(
                    target,
                    adventure,
                    messages.renderFor(
                        playerId,
                        MessageKey("core.language.reset"),
                        listOf(MessageArgument(resolver.effective(playerId).value)),
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LOGGER.warn("unable to clear language preference for {}", playerId, error)
                kernel.message(
                    target,
                    adventure,
                    messages.renderFor(playerId, MessageKey("core.language.storage_failed"))
                )
            }
        }
        return acknowledge(
            source,
            adventure,
            messages,
            playerId,
            job == null
        )
    }

    private fun acknowledge(
        source: CommandSourceStack,
        adventure: AdventureRuntime,
        messages: LocalizedMessages,
        playerId: UUID?,
        shuttingDown: Boolean,
    ): Int =
        when {
            shuttingDown -> source.replyError(
                adventure,
                messages.renderFor(playerId, MessageKey("core.error.runtime_stopping")),
            )

            else -> 1
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
                messages.renderFor(playerId, MessageKey("core.commands.not_ready"))
            )
        source.reply(
            adventure,
            messages.renderFor(playerId, MessageKey("core.commands.conflicts_title"))
        )
        if (report.conflicts.isEmpty()) {
            return source.reply(
                adventure,
                messages.renderFor(playerId, MessageKey("core.commands.conflicts_none"))
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

    private fun reload(
        source: CommandSourceStack,
        config: ConfigStore<CellulosesConfig>,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        onReloadApplied: suspend () -> Unit,
    ): Int {
        val playerId = source.player?.uuid
        val target = source.feedbackTarget()
        val job = kernel.launch {
            when (val result = config.reload()) {
                is ConfigReloadResult.Success -> {
                    onReloadApplied()
                    kernel.message(
                        target,
                        adventure,
                        messages.renderFor(
                            playerId,
                            MessageKey("core.reload.succeeded"),
                            listOf(MessageArgument(result.generation.toString())),
                        ),
                    )
                }

                is ConfigReloadResult.Failure -> kernel.message(
                    target,
                    adventure,
                    messages.renderFor(
                        playerId,
                        MessageKey("core.reload.failed"),
                        listOf(
                            MessageArgument(
                                result.errors.joinToString("; ") {
                                    "${it.path}: ${it.message}"
                                }
                            )
                        ),
                    ),
                )
            }
        }
        return when (job) {
            null -> source.replyError(
                adventure,
                messages.renderFor(playerId, MessageKey("core.error.runtime_stopping")),
            )

            else -> source.reply(
                adventure,
                messages.renderFor(playerId, MessageKey("core.reload.pending"))
            )
        }
    }

}
