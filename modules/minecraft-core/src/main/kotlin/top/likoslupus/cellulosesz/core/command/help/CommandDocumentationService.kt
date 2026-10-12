package top.likoslupus.cellulosesz.core.command.help

import net.kyori.adventure.text.Component as AdventureComponent
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.commands.CommandSourceStack
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.command.registry.CommandUsage
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.document.AdventureDocumentRenderer
import top.likoslupus.cellulosesz.core.text.document.DocumentLoadResult
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme
import top.likoslupus.cellulosesz.core.text.document.MarkdownDocumentLoader
import top.likoslupus.cellulosesz.core.text.i18n.BundledTranslations
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import java.util.*

/**
 * Renders a resolved command document for a source: it loads the bundled Markdown for the effective
 * locale and prepends the executable usage projection. The whole document is emitted at once (no
 * pagination). It never queries storage and never executes a command.
 */
public class CommandDocumentationService(
    private val loader: MarkdownDocumentLoader,
    private val messages: LocalizedMessages,
    private val languages: LanguageResolver,
    private val permissions: PermissionService,
) {

    public fun render(
        resolution: HelpResolution.Owned,
        source: CommandSourceStack,
        theme: DocumentTheme,
    ): List<AdventureComponent> {
        val playerId = source.player?.uuid

        if (!resolution.permitted) {
            return listOf(
                messages.renderFor(playerId, MessageKey("help.document.restricted"))
            )
        }

        val document = when (val result = loadDocument(resolution.documentation, playerId)) {
            is DocumentLoadResult.Found -> result.document

            DocumentLoadResult.Missing ->return listOf(
                messages.renderFor(
                    playerId,
                    MessageKey("help.document.missing"),
                    resolution.definition.name,
                )
            )

            is DocumentLoadResult.Invalid -> {
                LOGGER.warn(
                    "invalid help document '{}': {}",
                    resolution.definition.documentation.value,
                    result.reason,
                )
                return listOf(
                    messages.renderFor(
                        playerId,
                        MessageKey("help.document.missing"),
                        resolution.definition.name,
                    )
                )
            }
        }

        val out = mutableListOf<AdventureComponent>()
        out += messages.renderFor(playerId, MessageKey("help.usage"))
                .decorate(TextDecoration.BOLD)
        CommandUsage.patterns(resolution.definition, source, permissions)
                .forEach { out += AdventureComponent.text(it, theme.body) }
        out += AdventureComponent.empty()
        out += AdventureDocumentRenderer(theme).render(document)
        return out
    }

    private fun loadDocument(id: CommandDocumentId, playerId: UUID?): DocumentLoadResult {
        val order = listOf(
            languages.effective(playerId),
            languages.serverDefault(),
            BundledTranslations.baseline,
        ).distinct()

        var invalid: DocumentLoadResult.Invalid? = null
        order.forEach { language ->
            when (val result = loader.load(id, language)) {
                is DocumentLoadResult.Found -> return result
                is DocumentLoadResult.Invalid -> if (invalid == null) invalid = result
                DocumentLoadResult.Missing -> Unit
            }
        }
        return invalid ?: DocumentLoadResult.Missing
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(CommandDocumentationService::class.java)

    }

}
