package top.likoslupus.cellulosesz.core.text.document

/** A parsed command document, independent of any theme or locale. Cached and re-rendered. */
public class MarkdownDocument public constructor(
    public val blocks: List<MarkdownBlock>,
)

/** A block-level element of a command document. */
public sealed interface MarkdownBlock {

    public data class Heading(
        public val level: Int,
        public val spans: List<MarkdownSpan>,
    ) : MarkdownBlock

    public data class Paragraph(
        public val spans: List<MarkdownSpan>,
    ) : MarkdownBlock

    public data class BulletItem(
        public val depth: Int,
        public val spans: List<MarkdownSpan>,
    ) : MarkdownBlock

    public data class OrderedItem(
        public val depth: Int,
        public val number: Int,
        public val spans: List<MarkdownSpan>,
    ) : MarkdownBlock

    public data class CodeBlock(
        public val lines: List<String>,
    ) : MarkdownBlock

    public data class Quote(
        public val blocks: List<MarkdownBlock>,
    ) : MarkdownBlock

    public data object Divider : MarkdownBlock

}

/** An inline element of a command document. */
public sealed interface MarkdownSpan {

    public data class Text(
        public val value: String,
        public val bold: Boolean = false,
        public val italic: Boolean = false,
    ) : MarkdownSpan

    public data class Code(
        public val value: String,
    ) : MarkdownSpan

    public data class Link(
        public val label: String,
        public val target: String,
        public val bold: Boolean = false,
        public val italic: Boolean = false,
    ) : MarkdownSpan

}

/** Raised when a document uses a construct the command help renderer deliberately does not support. */
public class MarkdownDocumentException(
    message: String,
) : RuntimeException(message)
