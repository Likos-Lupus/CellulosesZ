package top.likoslupus.cellulosesz.core.text.document

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * Renders a parsed [MarkdownDocument] into layout blocks of chat lines. It never runs a command: a
 * `/command` link becomes `suggestCommand`, an `https://` link becomes `openUrl`, and any other
 * target is rendered as plain text. Console output is still readable because no line depends on
 * hover.
 */
public class AdventureDocumentRenderer(
    private val theme: DocumentTheme,
) {

    public fun render(document: MarkdownDocument): List<Component> =
        renderBlocks(document).flatMap { it.lines }

    /** Block-level rendering used by the paginator; internal so the public surface stays small. */
    internal fun renderBlocks(document: MarkdownDocument): List<RenderedBlock> {
        val out = mutableListOf<RenderedBlock>()
        document.blocks.forEach { appendBlock(it, depth = 0, out = out) }
        return out
    }

    private fun appendBlock(
        block: MarkdownBlock,
        depth: Int,
        out: MutableList<RenderedBlock>
    ) {
        when (block) {
            is MarkdownBlock.Heading ->
                out += RenderedBlock(
                    lines = listOf(
                        line(
                            block.spans,
                            theme.heading
                        ).decorate(TextDecoration.BOLD)
                    ),
                    keepWithNext = true,
                )

            is MarkdownBlock.Paragraph ->
                out += RenderedBlock(
                    lines = listOf(
                        line(
                            block.spans,
                            theme.body
                        )
                    )
                )

            is MarkdownBlock.BulletItem -> {
                val prefix = Component.text(
                    "${"  ".repeat(block.depth)}• ",
                    theme.heading
                )
                out += RenderedBlock(
                    lines = listOf(
                        prefix.append(
                            line(
                                block.spans,
                                theme.body
                            )
                        )
                    )
                )
            }

            is MarkdownBlock.OrderedItem -> {
                val prefix = Component.text(
                    "${"  ".repeat(block.depth)}${block.number}. ",
                    theme.heading
                )
                out += RenderedBlock(
                    lines = listOf(
                        prefix.append(
                            line(
                                block.spans,
                                theme.body
                            )
                        )
                    )
                )
            }

            is MarkdownBlock.CodeBlock ->
                out += RenderedBlock(lines = block.lines.map {
                    Component.text(
                        "  $it",
                        theme.code
                    )
                })

            is MarkdownBlock.Quote -> {
                val inner = mutableListOf<RenderedBlock>()
                block.blocks.forEach { appendBlock(it, depth + 1, inner) }
                val lines = inner.flatMap { it.lines }.map {
                    Component.text("> ", theme.divider).append(it)
                }
                out += RenderedBlock(lines = lines)
            }

            is MarkdownBlock.Callout -> {
                val inner = mutableListOf<RenderedBlock>()
                block.blocks.forEach { appendBlock(it, depth + 1, inner) }
                val color = when (block.kind) {
                    CalloutKind.TIP -> theme.tip
                    CalloutKind.NOTE -> theme.note
                    CalloutKind.WARNING -> theme.warning
                }
                val marker = Component.text("▌ ", color)
                inner.flatMap { it.lines }
                        .forEach { out += RenderedBlock(lines = listOf(marker.append(it))) }
            }

            MarkdownBlock.Divider ->
                out += RenderedBlock(
                    lines = listOf(
                        Component.text(
                            "─".repeat(16),
                            theme.divider
                        )
                    )
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
