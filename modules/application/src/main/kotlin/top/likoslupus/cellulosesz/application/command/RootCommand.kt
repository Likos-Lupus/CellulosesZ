package top.likoslupus.cellulosesz.application.command

import kotlinx.coroutines.CancellationException
import net.kyori.adventure.text.Component as AdventureComponent
import net.minecraft.commands.CommandSourceStack
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.message
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.*
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import top.likoslupus.cellulosesz.foundation.config.ConfigStore

internal object RootCommand {

    const val ROOT_LITERAL: String = "cellulosesz"

    private val LOGGER = LoggerFactory.getLogger(RootCommand::class.java)

    fun definition(
        config: ConfigStore<CellulosesConfig>,
        kernel: RuntimeKernel,
        health: ApplicationHealth,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        languages: PlayerLanguagePreferences,
        resolver: LanguageResolver,
        catalog: TranslationCatalog,
    ): CommandDefinition =
        command(
            name = ROOT_LITERAL,
            category = CommandCategory.ROOT,
            permission = CommandPermissions.ROOT,
            documentation = "core/cellulosesz",
        ) {
            literal("status", permission = CommandPermissions.ROOT_STATUS) {
                executes {
                    source.sendSuccess(
                        { Messages.prefixed(health.summary()) },
                        false,
                    )
                    1
                }
            }
            literal("reload", permission = CommandPermissions.ROOT_RELOAD) {
                executes {
                    reload(
                        source,
                        config,
                        kernel
                    )
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

        val component = messages.renderPrefixedFor(
            playerId,
            when {
                explicit != null -> CoreMessageKeys.LANGUAGE_CURRENT
                else -> CoreMessageKeys.LANGUAGE_CURRENT_INHERITED
            },
            listOf(MessageArgument(effective.value)),
        )

        return source.reply(adventure, component)
    }

    private fun listLanguages(
        source: CommandSourceStack,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        catalog: TranslationCatalog,
    ): Int {
        val listed = catalog.languages.joinToString(", ") { it.value }
        val component = messages.renderPrefixedFor(
            source.player?.uuid,
            CoreMessageKeys.LANGUAGE_AVAILABLE,
            listOf(MessageArgument(listed)),
        )
        return source.reply(adventure, component)
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
            ?: return source.replyError(adventure, requiresPlayer())

        val requested = LanguageId.parse(raw)
        if (requested == null || !catalog.supports(requested)) {
            val component = messages.renderPrefixedFor(
                player.uuid,
                CoreMessageKeys.LANGUAGE_UNKNOWN,
                listOf(MessageArgument(raw)),
            )
            return source.reply(adventure, component)
        }

        val target = source.feedbackTarget()
        val playerId = player.uuid
        val job = kernel.launch {
            try {
                languages.set(playerId, requested)
                kernel.message(
                    target,
                    adventure,
                    messages.renderPrefixedFor(
                        playerId,
                        CoreMessageKeys.LANGUAGE_CHANGED,
                        listOf(MessageArgument(requested.value)),
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LOGGER.warn("unable to persist language preference for {}", playerId, error)
                kernel.message(
                    target,
                    adventure,
                    messages.renderPrefixedFor(playerId, CoreMessageKeys.LANGUAGE_STORAGE_FAILED),
                )
            }
        }

        return acknowledge(source, adventure, job == null)
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
            ?: return source.replyError(adventure, requiresPlayer())

        val target = source.feedbackTarget()
        val playerId = player.uuid
        val job = kernel.launch {
            try {
                languages.reset(playerId)
                kernel.message(
                    target,
                    adventure,
                    messages.renderPrefixedFor(
                        playerId,
                        CoreMessageKeys.LANGUAGE_RESET,
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
                    messages.renderPrefixedFor(playerId, CoreMessageKeys.LANGUAGE_STORAGE_FAILED),
                )
            }
        }

        return acknowledge(source, adventure, job == null)
    }

    private fun acknowledge(
        source: CommandSourceStack,
        adventure: AdventureRuntime,
        shuttingDown: Boolean
    ): Int =
        when {
            shuttingDown -> source.replyError(
                adventure,
                AdventureComponent.text("${Messages.PREFIX}runtime is shutting down")
            )

            else -> 1
        }

    private fun requiresPlayer(): AdventureComponent =
        AdventureComponent.text("${Messages.PREFIX}this command requires a player")

    private fun reload(
        source: CommandSourceStack,
        config: ConfigStore<CellulosesConfig>,
        kernel: RuntimeKernel,
    ): Int {
        val job = kernel.launch {
            when (val result = config.reload()) {
                is ConfigReloadResult.Success -> kernel.onServerThread {
                    source.sendSuccess(
                        { Messages.prefixed("config reloaded (generation ${result.generation})") },
                        true,
                    )
                }

                is ConfigReloadResult.Failure -> kernel.onServerThread {
                    source.sendFailure(
                        Messages.prefixed(
                            "config reload failed: ${
                                result.errors.joinToString("; ") {
                                    "${it.path}: ${it.message}"
                                }
                            }"
                        )
                    )
                }
            }
        }
        return when (job) {
            null -> {
                source.sendFailure(Messages.prefixed("runtime is shutting down; reload rejected"))
                0
            }

            else -> {
                source.sendSuccess({ Messages.prefixed("reloading config...") }, false)
                1
            }
        }
    }

}
