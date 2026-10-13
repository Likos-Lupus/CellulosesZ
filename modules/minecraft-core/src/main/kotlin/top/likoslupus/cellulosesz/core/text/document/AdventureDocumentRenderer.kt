package top.likoslupus.cellulosesz.core.text.document

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import top.likoslupus.cellulosesz.core.text.theme.toLayoutStyle

/**
 * Renders a parsed [MarkdownDocument] into chat lines. It never runs a command: a `/command` link
 * becomes `suggestCommand`, an `https://` link becomes `openUrl`, and any other target is rendered as
 * plain text. Console output is still readable because no line depends on hover. On a comfortable
 * density a blank line separates top-level blocks; compact keeps them adjacent.
 */
public class AdventureDocumentRenderer(
    private val theme: DocumentTheme,
) {

    public fun render(document: MarkdownDocument): List<Component> {
        val spacing = theme.density.toLayoutStyle().sectionSpacing
        val out = mutableListOf<Component>()
        document.blocks.forEach { block ->
            if (spacing && out.isNotEmpty()) {
                out += Component.empty()
            }
            appendBlock(block, depth = 0, out = out)
        }
        return out
    }

    private fun appendBlock(
        block: MarkdownBlock,
        depth: Int,
        out: MutableList<Component>
    ) {
        when (block) {
            is MarkdownBlock.Heading ->
                out += line(block.spans, theme.heading)
                        .decorate(TextDecoration.BOLD)

            is MarkdownBlock.Paragraph ->
                out += line(block.spans, theme.body)

            is MarkdownBlock.ListBlock ->
                block.entries.forEach { entry ->
                    val prefix = when {
                        block.ordered -> Component.text(
                            "${"  ".repeat(entry.depth)}${entry.number ?: 1}. ",
                            theme.heading
                        )

                        else -> Component.text(
                            "${"  ".repeat(entry.depth)}• ",
                            theme.heading
                        )
                    }
                    out += prefix.append(line(entry.spans, theme.body))
                }

            is MarkdownBlock.CodeBlock ->
                block.lines.forEach {
                    out += Component.text(
                        "  $it",
                        theme.code
                    )
                }

            is MarkdownBlock.Quote -> {
                val inner = mutableListOf<Component>()
                block.blocks.forEach { appendBlock(it, depth + 1, inner) }
                inner.forEach {
                    out += Component.text(
                        "> ",
                        theme.divider
                    ).append(it)
                }
            }

            is MarkdownBlock.Callout -> {
                val color = when (block.kind) {
                    CalloutKind.TIP -> theme.tip
                    CalloutKind.NOTE -> theme.note
                    CalloutKind.WARNING -> theme.warning
                }
                val marker = Component.text("▌ ", color)
                val inner = mutableListOf<Component>()
                block.blocks.forEach { appendBlock(it, depth + 1, inner) }
                inner.forEach { out += marker.append(it) }
            }

            MarkdownBlock.Divider ->
                out += Component.text(
                    "─".repeat(16),
                    theme.divider
                )
        }
    }

    private fun line(spans: List<MarkdownSpan>, color: TextColor): Component {
        val builder = Component.text()
        spans.forEach { span ->
            when (span) {
                is MarkdownSpan.Text -> {
                    var part = Component.text(span.value, color)
                    if (span.bold) part = part.decorate(TextDecoration.BOLD)
                    if (span.italic) part = part.decorate(TextDecoration.ITALIC)
                    builder.append(part)
                }

                is MarkdownSpan.Code ->
                    builder.append(Component.text(span.value, theme.code))

                is MarkdownSpan.Link ->
                    builder.append(linkComponent(span))
            }
        }
        return builder.build()
    }

    private fun linkComponent(span: MarkdownSpan.Link): Component {
        var component = Component.text(span.label, theme.link)
        if (span.bold) component = component.decorate(TextDecoration.BOLD)
        if (span.italic) component = component.decorate(TextDecoration.ITALIC)

        return when {
            span.target.startsWith("/") ->
                component
                        .clickEvent(ClickEvent.suggestCommand(span.target))
                        .hoverEvent(
                            HoverEvent.showText(
                                Component.text(
                                    span.target,
                                    theme.link
                                )
                            )
                        )

            span.target.startsWith("https://") ->
                component.clickEvent(ClickEvent.openUrl(span.target))

            else -> component
        }
    }

}
