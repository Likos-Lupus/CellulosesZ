package top.likoslupus.cellulosesz.core.text.presentation

import top.likoslupus.cellulosesz.core.text.MessageKey

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.theme.TextRole

/**
 * Renders a [ListPage]. An empty collection is a neutral message, not an error. Row numbering is
 * textual, never space-padded alignment (client font widths are not fixed).
 */
internal class CollectionRenderer(
    private val messages: LocalizedMessages,
) {

    fun render(context: RenderContext, page: ListPage): List<Component> {
        val palette = context.theme.palette
        val out = mutableListOf<Component>()

        var title = messages
                .render(context.language, page.title)
                .decorate(TextDecoration.BOLD)
                .color(palette.color(TextRole.HEADING))
        page.page?.let { info ->
            title = title.append(
                Component.text(
                    "  ${info.index} / ${info.count}",
                    palette.color(TextRole.MUTED)
                )
            )
        }
        out += title
        page.summary?.let {
            out += messages.render(
                context.language,
                it
            )
        }

        if (page.rows.isEmpty()) {
            out += messages.render(
                context.language,
                page.emptyText
            )
        } else {
            page.rows.forEachIndexed { index, row ->
                val number = messages.renderColored(
                    context.language,
                    LocalizedText(
                        MessageKey("core.list.ordered"),
                        listOf(TextArgument.Count((index + 1).toLong())),
                    ),
                    palette.color(TextRole.MUTED),
                )
                val line = number
                        .append(Component.text(" "))
                        .append(
                            messages.render(
                                context.language,
                                row.main
                            )
                        )
                row.supporting?.let { supporting ->
                    out += line
                    out += Component
                            .text(
                                "   ",
                                palette.color(TextRole.MUTED)
                            )
                            .append(
                                messages.render(
                                    context.language,
                                    supporting
                                )
                            )
                    return@forEachIndexed
                }
                row.action?.let { action ->
                    out += line
                            .append(Component.text("  "))
                            .append(
                                ActionRenderer.render(
                                    messages,
                                    context,
                                    action
                                )
                            )
                } ?: run { out += line }
            }
        }

        val nav = mutableListOf<Component>()
        page.previous?.let {
            nav += ActionRenderer.render(
                messages,
                context,
                it
            )
        }
        page.next?.let {
            nav += ActionRenderer.render(
                messages,
                context,
                it
            )
        }
        if (nav.isNotEmpty()) {
            out += nav.reduce { acc, component ->
                acc
                        .append(Component.text("  "))
                        .append(component)
            }
        }
        return out
    }

}
