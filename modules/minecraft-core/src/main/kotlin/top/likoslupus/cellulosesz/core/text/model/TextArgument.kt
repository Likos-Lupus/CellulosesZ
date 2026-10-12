package top.likoslupus.cellulosesz.core.text.model

import kotlin.time.Duration

/**
 * A typed display argument. Non-literal kinds are formatted by the renderer using the recipient
 * language; every value is inserted as text and is never re-parsed as a template, Markdown or a
 * clickable command.
 */
public sealed interface TextArgument {

    /** Arbitrary text, always displayed literally. */
    public data class Literal(public val value: String) : TextArgument

    /** Another player's name. */
    public data class PlayerName(public val value: String) : TextArgument

    /** A stable identifier such as a home, warp or kit name. */
    public data class Identifier(public val value: String) : TextArgument

    /** A command sample; it is never executed by the renderer. */
    public data class Command(public val value: String) : TextArgument

    /** A quantity. */
    public data class Count(public val value: Long) : TextArgument

    /** A remaining or elapsed duration, formatted per locale by the renderer. */
    public data class Elapsed(public val value: Duration) : TextArgument

    /** A block position with an optional dimension label. */
    public data class Position(
        public val x: Int,
        public val y: Int,
        public val z: Int,
        public val dimension: String? = null,
    ) : TextArgument

}
