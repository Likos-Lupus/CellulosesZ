package top.likoslupus.cellulosesz.foundation.database

import kotlinx.serialization.Serializable
import java.security.MessageDigest

/** How a backend's password is obtained. Persisted in the `last-successful` sidecar. */
@Serializable
public enum class DatabaseSecretSource {

    LITERAL,
    ENVIRONMENT,

}

/**
 * A non-printing password reference. The concrete value is resolved on demand so it never appears
 * in logs, `toString`, or exception messages.
 */
@Serializable
public data class DatabaseSecretSpec(
    public val source: DatabaseSecretSource,
    public val literalValue: String? = null,
    public val envKey: String? = null,
) {

    public val reference: String
        get() =
            when (source) {
                DatabaseSecretSource.LITERAL -> "literal"
                DatabaseSecretSource.ENVIRONMENT -> "env:$envKey"
            }

    /**
     * Non-reversible fingerprint distinguishing different secrets/references without leaking them.
     * A rotated literal password produces a different fingerprint, so it counts as a connection
     * change (never as a data move).
     */
    public fun fingerprint(): String {
        val basis = when (source) {
            DatabaseSecretSource.LITERAL -> "literal:${literalValue.orEmpty()}"
            DatabaseSecretSource.ENVIRONMENT -> "env:${envKey.orEmpty()}"
        }
        return MessageDigest.getInstance("SHA-256")
                .digest(basis.toByteArray(Charsets.UTF_8))
                .joinToString("") {
                    "%02x".format(it)
                }
    }

    /** Resolves the actual password. Throws [IllegalStateException] if an env secret is missing. */
    public fun resolve(): String =
        when (source) {
            DatabaseSecretSource.LITERAL -> literalValue.orEmpty()
            DatabaseSecretSource.ENVIRONMENT -> {
                val key = envKey.orEmpty()
                System.getenv(key)
                    ?: throw IllegalStateException("required database password environment variable '$key' is not set")
            }
        }

    override fun toString(): String =
        "DatabaseSecretSpec(source=$source)"

    public companion object {

        public fun literal(value: String): DatabaseSecretSpec =
            DatabaseSecretSpec(
                DatabaseSecretSource.LITERAL,
                literalValue = value
            )

        public fun environment(key: String): DatabaseSecretSpec =
            DatabaseSecretSpec(
                DatabaseSecretSource.ENVIRONMENT,
                envKey = key
            )

        /** Chooses env when `passwordEnv` is set, otherwise an (possibly empty) literal password. */
        public fun of(password: String, passwordEnv: String): DatabaseSecretSpec =
            when {
                passwordEnv.isNotEmpty() -> environment(passwordEnv)
                else -> literal(password)
            }

    }

}
