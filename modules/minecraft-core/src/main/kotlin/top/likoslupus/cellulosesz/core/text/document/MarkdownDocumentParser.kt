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
                listItems(node, depth, ordered = false, out = out)

            is OrderedList ->
                listItems(node, depth, ordered = true, out = out)

            is BlockQuote -> {
                val inner = mutableListOf<MarkdownBlock>()
                blockChildren(node, depth, inner)
                out += MarkdownBlock.Quote(inner)
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

    private fun listItems(
        list: Node,
        depth: Int,
        ordered: Boolean,
        out: MutableList<MarkdownBlock>
    ) {
        var index = 1
        var item = list.firstChild
        while (item != null) {
            val next = item.next
            if (item is ListItem) {
                var first = true
                var sub = item.firstChild
                while (sub != null) {
                    val subNext = sub.next
                    when (sub) {
                        is Paragraph -> when {
                            first -> {
                                out += when {
                                    ordered -> MarkdownBlock.OrderedItem(depth, index, spans(sub))
                                    else -> MarkdownBlock.BulletItem(depth, spans(sub))
                                }
                                first = false
                            }

                            else -> out += MarkdownBlock.Paragraph(spans(sub))
                        }

                        is BulletList ->
                            listItems(sub, depth + 1, ordered = false, out = out)

                        is OrderedList ->
                            listItems(sub, depth + 1, ordered = true, out = out)

                        else -> appendBlock(sub, depth + 1, out)
                    }
                    sub = subNext
                }
                index++
            }
            item = next
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
