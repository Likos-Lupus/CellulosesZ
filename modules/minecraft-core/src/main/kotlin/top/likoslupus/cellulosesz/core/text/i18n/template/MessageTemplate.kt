package top.likoslupus.cellulosesz.core.text.i18n.template

/** The two semantic message roles. Translations express meaning, not RGB values. */
public enum class MessageRole {

    PRIMARY,
    SECONDARY,

}

/** A compiled template fragment. This is deliberately not a Markdown-like document model. */
internal sealed interface MessagePart {

    val role: MessageRole

    data class Text(
        val value: String,
        override val role: MessageRole,
    ) : MessagePart

    data class Argument(
        val index: Int,
        override val role: MessageRole,
    ) : MessagePart

}

/**
 * A translation compiled once at bootstrap. [argumentIndexes] lets the catalog validate arity across
 * languages without touching the raw translation text again.
 */
public class MessageTemplate internal constructor(
    internal val parts: List<MessagePart>,
    public val argumentIndexes: Set<Int>,
)
