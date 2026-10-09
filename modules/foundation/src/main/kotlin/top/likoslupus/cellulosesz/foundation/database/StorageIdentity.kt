package top.likoslupus.cellulosesz.foundation.database

import kotlinx.serialization.Serializable
import java.security.MessageDigest

/**
 * Identifies *where* the business data physically lives. It intentionally excludes credentials and
 * pool/timeout options, so a password or pool change is never mistaken for a data move.
 */
@Serializable
public data class StorageIdentity(
    public val type: DatabaseType,
    public val namespace: String,
    public val host: String? = null,
    public val port: Int? = null,
    public val database: String? = null,
    public val schema: String? = null,
    public val path: String? = null,
) {

    /** Stable, human-inspectable key used as the migration identity basis. */
    public fun canonical(): String = when (type) {
        DatabaseType.SQLITE -> "sqlite|$namespace|path=$path"
        DatabaseType.H2 -> "h2|$namespace|path=$path"
        DatabaseType.MYSQL -> "mysql|$namespace|$host:$port/$database"
        DatabaseType.MARIADB -> "mariadb|$namespace|$host:$port/$database"
        DatabaseType.POSTGRESQL -> "postgresql|$namespace|$host:$port/$database/$schema"
    }

    public fun hash(): String =
        sha256Hex(canonical())

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }

}
