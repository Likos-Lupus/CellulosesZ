package top.likoslupus.cellulosesz.foundation.config

/** A single validation failure, addressed by its (dotted) config path. */
public data class ValidationError(
    public val path: String,
    public val message: String,
)
