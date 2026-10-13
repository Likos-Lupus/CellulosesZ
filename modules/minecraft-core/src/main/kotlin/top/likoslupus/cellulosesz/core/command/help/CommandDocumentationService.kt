package top.likoslupus.cellulosesz.core.command.help

import net.kyori.adventure.text.Component as AdventureComponent
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.commands.CommandSourceStack
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.command.registry.CommandUsage
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.document.*
import top.likoslupus.cellulosesz.core.text.i18n.BundledTranslations
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.theme.toLayoutStyle
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

            DocumentLoadResult.Missing -> return listOf(
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
                        MessageKey("help.document.invalid")
                    )
                )
            }
        }

        val blocks = document.blocks
        val title = blocks.getOrNull(0) as? MarkdownBlock.Heading
        val summary = blocks.getOrNull(1) as? MarkdownBlock.Paragraph
        val head = when {
            title != null && summary != null -> listOf(title, summary)
            title != null -> listOf(title)
            else -> emptyList()
        }
        val rest = blocks.drop(head.size)

        val renderer = AdventureDocumentRenderer(theme)
        val spacing = theme.density.toLayoutStyle().sectionSpacing
        val out = mutableListOf<AdventureComponent>()
        if (head.isNotEmpty()) {
            out += renderer.render(MarkdownDocument(head))
            if (spacing) {
                out += AdventureComponent.empty()
            }
        }
        out += messages.renderFor(playerId, MessageKey("help.usage"))
                .decorate(TextDecoration.BOLD)
        CommandUsage.patterns(resolution.definition, source, permissions)
                .forEach { out += AdventureComponent.text(it, theme.body) }
        if (spacing) {
            out += AdventureComponent.empty()
        }
        out += renderer.render(MarkdownDocument(rest))
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
