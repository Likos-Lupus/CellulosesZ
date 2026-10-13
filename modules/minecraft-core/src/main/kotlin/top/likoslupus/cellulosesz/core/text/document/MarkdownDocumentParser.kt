package top.likoslupus.cellulosesz.core.text.document

import org.commonmark.node.*
import org.commonmark.parser.Parser

/**
 * Converts CommonMark into the small [MarkdownDocument] model used by the command help renderer. It
 * intentionally rejects raw HTML and images rather than smuggling them into Adventure.
 */
public object MarkdownDocumentParser {

    private val parser: Parser = Parser.builder().build()

    public fun parse(markdown: String): MarkdownDocument {
        val root = parser.parse(markdown)
        val blocks = mutableListOf<MarkdownBlock>()
        blockChildren(root, depth = 0, out = blocks)
        return MarkdownDocument(blocks)
    }

    private fun blockChildren(
        parent: Node,
        depth: Int,
        out: MutableList<MarkdownBlock>
    ) {
        var child = parent.firstChild
        while (child != null) {
            val next = child.next
            appendBlock(child, depth, out)
            child = next
        }
    }

    private fun appendBlock(
        node: Node,
        depth: Int,
        out: MutableList<MarkdownBlock>
    ) {
        when (node) {
            is Heading ->
                out += MarkdownBlock.Heading(node.level, spans(node))

            is Paragraph ->
                out += MarkdownBlock.Paragraph(spans(node))

            is FencedCodeBlock ->
                out += MarkdownBlock.CodeBlock(codeLines(node.literal))

            is IndentedCodeBlock ->
                out += MarkdownBlock.CodeBlock(codeLines(node.literal))

            is BulletList ->
                out += listBlock(node, depth, ordered = false)

            is OrderedList ->
                out += listBlock(node, depth, ordered = true)

            is BlockQuote -> {
                val kind = calloutKind(node)
                if (kind == null) {
                    val inner = mutableListOf<MarkdownBlock>()
                    blockChildren(node, depth, inner)
                    out += MarkdownBlock.Quote(inner)
                } else {
                    stripCalloutMarker(node)
                    val inner = mutableListOf<MarkdownBlock>()
                    blockChildren(node, depth, inner)
                    out += MarkdownBlock.Callout(kind, inner)
                }
            }

            is ThematicBreak ->
                out += MarkdownBlock.Divider

            is HtmlBlock ->
                throw MarkdownDocumentException("raw HTML blocks are not supported")

            is HtmlInline ->
                throw MarkdownDocumentException("raw HTML is not supported")

            is Image ->
                throw MarkdownDocumentException("images are not supported")

            else -> blockChildren(node, depth, out)
        }
    }

    private fun listBlock(
        list: Node,
        depth: Int,
        ordered: Boolean
    ): MarkdownBlock.ListBlock {
        val entries = mutableListOf<MarkdownBlock.ListEntry>()
        val first = if (ordered && list is OrderedList) list.markerStartNumber else 1
        collectEntries(list, depth, ordered, firstNumber = first, out = entries)
        return MarkdownBlock.ListBlock(ordered, entries)
    }

    private fun collectEntries(
        list: Node,
        depth: Int,
        ordered: Boolean,
        firstNumber: Int,
        out: MutableList<MarkdownBlock.ListEntry>
    ) {
        var index = firstNumber
        var item = list.firstChild
        while (item != null) {
            val next = item.next
            if (item is ListItem) {
                var lead: List<MarkdownSpan>? = null
                var sub = item.firstChild
                while (sub != null) {
                    if (sub is Paragraph && lead == null) {
                        lead = spans(sub)
                    }
                    sub = sub.next
                }
                out += MarkdownBlock.ListEntry(
                    depth = depth,
                    number = if (ordered) index else null,
                    spans = lead ?: emptyList(),
                )
                index++

                var nested = item.firstChild
                while (nested != null) {
                    when (nested) {
                        is BulletList ->
                            collectEntries(
                                nested,
                                depth + 1,
                                ordered = false,
                                firstNumber = 1,
                                out = out
                            )

                        is OrderedList ->
                            collectEntries(
                                nested,
                                depth + 1,
                                ordered = true,
                                firstNumber = nested.markerStartNumber,
                                out = out,
                            )

                        else -> Unit
                    }
                    nested = nested.next
                }
            }
            item = next
        }
    }

    private val CALLOUT_MARKER = Regex(
        "^\\[!(TIP|NOTE|WARNING)]\\s*",
        RegexOption.IGNORE_CASE
    )

    private fun calloutKind(quote: BlockQuote): CalloutKind? {
        val paragraph = quote.firstChild as? Paragraph
            ?: return null
        val text = paragraph.firstChild as? Text
            ?: return null
        val match = CALLOUT_MARKER.find(text.literal)
            ?: return null
        return when (match.groupValues[1].uppercase()) {
            "TIP" -> CalloutKind.TIP
            "NOTE" -> CalloutKind.NOTE
            "WARNING" -> CalloutKind.WARNING
            else -> null
        }
    }

    private fun stripCalloutMarker(quote: BlockQuote) {
        val paragraph = quote.firstChild as? Paragraph
            ?: return
        val text = paragraph.firstChild as? Text
            ?: return
        val match = CALLOUT_MARKER.find(text.literal)
            ?: return
        val remainder = text.literal.substring(match.range.last + 1).trimStart()
        if (remainder.isEmpty()) {
            val after = text.next
            text.unlink()
            if (after is SoftLineBreak || after is HardLineBreak) {
                after.unlink()
            }
            if (paragraph.firstChild == null) {
                paragraph.unlink()
            }
        } else {
            text.literal = remainder
        }
    }

    private fun codeLines(literal: String?): List<String> {
        val text = literal
            ?: return emptyList()
        val lines = text.split('\n')
        return when {
            lines.isNotEmpty() && lines.last().isEmpty() -> lines.dropLast(1)
            else -> lines
        }
    }

    private fun spans(parent: Node): List<MarkdownSpan> {
        val out = mutableListOf<MarkdownSpan>()
        inline(
            parent,
            bold = false,
            italic = false,
            out = out
        )
        return out
    }

    private fun inline(
        node: Node,
        bold: Boolean,
        italic: Boolean,
        out: MutableList<MarkdownSpan>
    ) {
        var child = node.firstChild
        while (child != null) {
            val next = child.next
            when (child) {
                is Text ->
                    out += MarkdownSpan.Text(child.literal, bold, italic)

                is Code ->
                    out += MarkdownSpan.Code(child.literal)

                is Emphasis ->
                    inline(child, bold, true, out)

                is StrongEmphasis ->
                    inline(child, true, italic, out)

                is Link ->
                    out += MarkdownSpan.Link(
                        label(child, bold, italic),
                        child.destination,
                        bold,
                        italic
                    )

                is SoftLineBreak ->
                    out += MarkdownSpan.Text(" ", bold, italic)

                is HardLineBreak ->
                    out += MarkdownSpan.Text(" ", bold, italic)

                is Image ->
                    throw MarkdownDocumentException("images are not supported")

                is HtmlInline ->
                    throw MarkdownDocumentException("raw HTML is not supported")

                else -> inline(child, bold, italic, out)
            }
            child = next
        }
    }

    private fun label(
        node: Node,
        bold: Boolean,
        italic: Boolean
    ): String {
        val spans = mutableListOf<MarkdownSpan>()
        inline(node, bold, italic, spans)
        return spans.joinToString("") {
            when (it) {
                is MarkdownSpan.Text -> it.value
                is MarkdownSpan.Code -> it.value
                is MarkdownSpan.Link -> it.label
            }
        }
    }

}
