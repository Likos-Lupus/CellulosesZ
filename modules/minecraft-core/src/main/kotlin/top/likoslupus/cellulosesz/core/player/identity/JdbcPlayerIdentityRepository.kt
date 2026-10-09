package top.likoslupus.cellulosesz.core.player.identity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.*

/** JDBC Player Identity Index. Never blocks the command path: it is only used at boot and on join. */
public class JdbcPlayerIdentityRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : PlayerIdentityRepository {

    override suspend fun loadAll(): List<PersistedPlayerIdentity> =
        withContext(Dispatchers.IO) { loadAllBlocking() }

    /** Blocking read used by the bounded startup bootstrap. */
    public fun loadAllBlocking(): List<PersistedPlayerIdentity> =
        try {
            database.read { connection -> readAll(connection) }
        } catch (exception: SQLException) {
            throw StorageException("unable to load player identity index", exception)
        }

    override suspend fun observe(
        identity: KnownPlayerIdentity,
        at: Instant
    ): Unit =
        withContext(Dispatchers.IO) {
            val normalized = normalize(identity.name)
            try {
                database.transaction { connection ->
                    val existingFirst = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            first_seen_ms
                        FROM
                            ${IdentitySqlSchema.PLAYERS}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, identity.id.toString())
                        statement.executeQuery().use { rs ->
                            when {
                                rs.next() -> rs.getLong(1)
                                else -> null
                            }
                        }
                    } ?: at.toEpochMilli()

                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${IdentitySqlSchema.PLAYERS}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, identity.id.toString())
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${IdentitySqlSchema.PLAYERS} (
                            namespace,
                            player_uuid,
                            current_name,
                            normalized_name,
                            first_seen_ms,
                            last_seen_ms
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, identity.id.toString())
                        statement.setString(3, identity.name)
                        statement.setString(4, normalized)
                        statement.setLong(5, existingFirst)
                        statement.setLong(6, at.toEpochMilli())
                        statement.executeUpdate()
                    }

                    val nameFirst = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            first_seen_ms
                        FROM
                            ${IdentitySqlSchema.PLAYER_NAMES}
                        WHERE
                            namespace = ? AND player_uuid = ? AND normalized_name = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, identity.id.toString())
                        statement.setString(3, normalized)
                        statement.executeQuery().use { rs ->
                            when {
                                rs.next() -> rs.getLong(1)
                                else -> null
                            }
                        }
                    } ?: at.toEpochMilli()

                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${IdentitySqlSchema.PLAYER_NAMES}
                        WHERE
                            namespace = ? AND player_uuid = ? AND normalized_name = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, identity.id.toString())
                        statement.setString(3, normalized)
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${IdentitySqlSchema.PLAYER_NAMES} (
                            namespace,
                            player_uuid,
                            observed_name,
                            normalized_name,
                            first_seen_ms,
                            last_seen_ms
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, identity.id.toString())
                        statement.setString(3, identity.name)
                        statement.setString(4, normalized)
                        statement.setLong(5, nameFirst)
                        statement.setLong(6, at.toEpochMilli())
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw StorageException(
                    "unable to record player identity for ${identity.id}",
                    exception
                )
            }
        }

    private fun readAll(connection: Connection): List<PersistedPlayerIdentity> =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                player_uuid,
                current_name,
                first_seen_ms,
                last_seen_ms
            FROM
                ${IdentitySqlSchema.PLAYERS}
            WHERE
                namespace = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val id = try {
                            UUID.fromString(rs.getString(1))
                        } catch (_: IllegalArgumentException) {
                            throw StorageException("identity index contains an invalid player uuid")
                        }

                        add(
                            PersistedPlayerIdentity(
                                id = id,
                                name = rs.getString(2),
                                firstSeen = Instant.ofEpochMilli(rs.getLong(3)),
                                lastSeen = Instant.ofEpochMilli(rs.getLong(4)),
                            )
                        )
                    }
                }
            }
        }

    private fun normalize(name: String): String =
        name.lowercase(Locale.ROOT)

}
