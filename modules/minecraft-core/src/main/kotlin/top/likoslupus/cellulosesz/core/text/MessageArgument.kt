package top.likoslupus.cellulosesz.core.text

/**
 * A positional template argument. Its value is always inserted as literal text: it can never be
 * re-parsed as template source, so user input cannot gain formatting semantics.
 */
@JvmInline
public value class MessageArgument(
    public val value: String,
)
