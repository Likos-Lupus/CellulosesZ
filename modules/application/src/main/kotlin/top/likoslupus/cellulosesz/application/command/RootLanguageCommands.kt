package top.likoslupus.cellulosesz.application.command

import net.minecraft.commands.CommandSourceStack
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.core.command.dsl.CommandSyntax
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.feedbackTarget
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
import java.util.*
import kotlinx.coroutines.CancellationException

private val LOGGER = LoggerFactory.getLogger(RootCommand::class.java)

/** `/cellulosesz language [list|server|<language>]`. */
internal fun CommandSyntax.languageCommands(
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    languages: PlayerLanguagePreferences,
    resolver: LanguageResolver,
    catalog: TranslationCatalog,
) {
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
