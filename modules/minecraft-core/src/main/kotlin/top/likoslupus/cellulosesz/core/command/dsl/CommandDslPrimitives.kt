package top.likoslupus.cellulosesz.core.command.dsl

/** Marks the receiver scopes of the command declaration DSL so nested blocks cannot call outward. */
@DslMarker
public annotation class CommandDslMarker

/** Which command sources may enter a path. */
public enum class SourceAccess {

    /** Any source (player, console, command block, RCON). */
    ANY,

    /** Only a player source may traverse the path. */
    PLAYER,

    /** Only a non-player source may traverse the path; the path is hidden from players. */
    NON_PLAYER,

}

/**
 * A stable, validated identifier for a Markdown documentation page, e.g. `movement/home`. It is a
 * relative resource path fragment, never an absolute path or URL.
 */
@JvmInline
public value class CommandDocumentId public constructor(public val value: String) {

    public companion object {

        private val PATTERN: Regex = Regex("[a-z0-9]+(?:[/_-][a-z0-9]+)*")

        public fun of(raw: String): CommandDocumentId {
            require(PATTERN.matches(raw)) {
                "invalid command documentation id '$raw'"
            }
            return CommandDocumentId(raw)
        }

    }

}
