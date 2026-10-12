package top.likoslupus.cellulosesz.core.command.help

import net.kyori.adventure.text.Component as AdventureComponent
import net.kyori.adventure.text.event.ClickEvent
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
import java.util.*

/** One rendered page of command documentation. */
public class HelpResult(
    public val lines: List<AdventureComponent>,
    public val page: Int,
    public val pageCount: Int,
)

/**
 * Renders a resolved command document for a source: it loads the bundled Markdown for the effective
 * locale, prepends the executable usage projection, paginates over layout blocks and appends a
 * clickable navigation line. It never queries storage and never executes a command.
 */
public class CommandDocumentationService(
    private val loader: MarkdownDocumentLoader,
    private val messages: LocalizedMessages,
    private val languages: LanguageResolver,
    private val permissions: PermissionService,
) {

    public fun page(
        resolution: HelpResolution.Owned,
        source: CommandSourceStack,
        theme: DocumentTheme,
    ): HelpResult {
        val playerId = source.player?.uuid

        if (!resolution.permitted) {
            return single(
                messages.renderFor(
                    playerId,
                    MessageKey("help.document.restricted")
                )
            )
        }

        val document = when (val result = loadDocument(resolution.documentation, playerId)) {
            is DocumentLoadResult.Found -> result.document

            DocumentLoadResult.Missing -> return single(
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
                return single(
                    messages.renderFor(
                        playerId,
                        MessageKey("help.document.missing"),
                        resolution.definition.name,
                    )
                )
            }
        }

        val blocks = mutableListOf<RenderedBlock>()
        val usageHeader = messages.renderFor(playerId, MessageKey("help.usage"))
                .decorate(TextDecoration.BOLD)
        val patternLines = CommandUsage.patterns(resolution.definition, source, permissions)
                .map {
                    AdventureComponent.text(
                        it,
                        theme.body
                    )
                }
        blocks += RenderedBlock(lines = listOf(usageHeader) + patternLines)
        blocks += RenderedBlock(lines = listOf(AdventureComponent.empty()))
        blocks += AdventureDocumentRenderer(theme).renderBlocks(document)

        val pages = DocumentPaginator.paginate(blocks)
        val clamped = resolution.page.coerceIn(1, pages.size)
        val page = pages[clamped - 1].toMutableList()
        page += if (clamped != resolution.page) {
            messages.renderFor(
                playerId,
                MessageKey("help.document.invalid_page"),
                resolution.page.toString(),
            )
        } else {
            navigation(
                playerId,
                resolution.canonical,
                clamped,
                pages.size,
                theme
            )
        }

        return HelpResult(
            page,
            clamped,
            pages.size
        )
    }

    private fun single(component: AdventureComponent): HelpResult =
        HelpResult(listOf(component), 1, 1)

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
        return invalid
            ?: DocumentLoadResult.Missing
    }

    private fun navigation(
        playerId: UUID?,
        canonical: String,
        page: Int,
        pageCount: Int,
        theme: DocumentTheme,
    ): AdventureComponent {
        val builder = AdventureComponent.text()
        if (page > 1) {
            builder.append(
                pageLink(
                    "‹",
                    canonical,
                    page - 1,
                    theme
                )
            )
        }
        builder.append(
            messages.renderFor(
                playerId,
                MessageKey("help.page"),
                page.toString(),
                pageCount.toString(),
            )
        )
        if (page < pageCount) {
            builder.append(
                pageLink(
                    "›",
                    canonical,
                    page + 1,
                    theme
                )
            )
        }
        return builder.build()
    }

    private fun pageLink(
        label: String,
        canonical: String,
        target: Int,
        theme: DocumentTheme,
    ): AdventureComponent =
        AdventureComponent
                .text(" $label ", theme.link)
                .clickEvent(ClickEvent.suggestCommand("/help $canonical --page $target"))

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(CommandDocumentationService::class.java)

    }

}
