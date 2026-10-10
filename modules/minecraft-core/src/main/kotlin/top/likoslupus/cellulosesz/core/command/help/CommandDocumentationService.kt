package top.likoslupus.cellulosesz.core.command.help

import net.kyori.adventure.text.Component as AdventureComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.command.registry.CommandUsage
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.document.AdventureDocumentRenderer
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme
import top.likoslupus.cellulosesz.core.text.document.MarkdownDocument
import top.likoslupus.cellulosesz.core.text.document.MarkdownDocumentLoader
import top.likoslupus.cellulosesz.core.text.i18n.*
import java.util.*

/** One rendered page of command documentation. */
public class HelpResult(
    public val lines: List<AdventureComponent>,
    public val page: Int,
    public val pageCount: Int,
)

/**
 * Renders a resolved command document for a source: it loads the bundled Markdown for the effective
 * locale, prepends the executable usage projection, paginates and appends a clickable navigation
 * line. It never queries storage and never executes a command.
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
            return HelpResult(
                listOf(
                    messages.renderFor(
                        playerId,
                        CoreMessageKeys.HELP_DOCUMENT_RESTRICTED
                    )
                ),
                1,
                1,
            )
        }

        val document = loadDocument(resolution.definition.documentation, playerId)
            ?: return HelpResult(
                listOf(
                    messages.renderFor(
                        playerId,
                        CoreMessageKeys.HELP_DOCUMENT_MISSING,
                        resolution.definition.name,
                    )
                ),
                1,
                1,
            )

        val lines = mutableListOf<AdventureComponent>()
        lines += messages.renderFor(playerId, CoreMessageKeys.HELP_USAGE)
                .decorate(TextDecoration.BOLD)
        CommandUsage.patterns(resolution.definition, source, permissions)
                .forEach {
                    lines += AdventureComponent.text(
                        it,
                        theme.body
                    )
                }
        lines += AdventureComponent.empty()
        lines += AdventureDocumentRenderer(theme).render(document)

        val pages = CommandHelpPaginator.paginate(lines)
        val clamped = resolution.page.coerceIn(1, pages.size)
        val page = pages[clamped - 1].toMutableList()
        page += if (clamped != resolution.page) {
            messages.renderFor(
                playerId,
                CoreMessageKeys.HELP_DOCUMENT_INVALID_PAGE,
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

    private fun loadDocument(id: CommandDocumentId, playerId: UUID?): MarkdownDocument? =
        loader.load(id, languages.effective(playerId))
            ?: loader.load(id, languages.serverDefault())
            ?: loader.load(id, BundledTranslations.baseline)

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
                CoreMessageKeys.HELP_PAGE,
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

}
