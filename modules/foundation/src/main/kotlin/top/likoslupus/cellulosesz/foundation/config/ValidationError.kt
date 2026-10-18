package top.likoslupus.cellulosesz.foundation.config

/**
 * A single validation failure, addressed by its (dotted) config path. [source] is the owning file for
 * a multi-file configuration and `null` for a single-file store or a test.
 */
public data class ValidationError(
    public val path: String,
    public val message: String,
    public val source: String? = null,
)

/** Renders a failure as `file: path: message` when a source is known. */
public fun ValidationError.describe(): String =
    when (source) {
        null -> "$path: $message"
        else -> "$source: $path: $message"
    }
