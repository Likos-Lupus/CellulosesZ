package top.likoslupus.cellulosesz.core.text.i18n.preference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.sql.Connection
import java.sql.SQLException
import java.util.*

/** JDBC-backed language preference repository. Never touched on the message render path. */
public class JdbcPlayerLanguagePreferenceRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : PlayerLanguagePreferenceRepository {

    override suspend fun loadAll(): Map<UUID, LanguageId> =
        withContext(Dispatchers.IO) { loadAllBlocking() }

    /** Blocking read used by the bounded startup bootstrap. */
    public fun loadAllBlocking(): Map<UUID, LanguageId> =
        try {
            database.read { connection -> readAll(connection) }
        } catch (exception: SQLException) {
            throw StorageException("unable to load player language preferences", exception)
        }

    override suspend fun set(
        playerId: UUID,
        language: LanguageId
    ): Unit =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    delete(connection, playerId)
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${LocalizationSqlSchema.PLAYER_LANGUAGES} (
                            namespace,
                            player_uuid,
                            language
                        )
                        VALUES (
                            ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.setString(3, language.value)
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw StorageException(
                    "unable to record language preference for $playerId",
                    exception
                )
            }
        }

    override suspend fun remove(playerId: UUID): Unit =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    delete(connection, playerId)
                }
            } catch (exception: SQLException) {
                throw StorageException(
                    "unable to clear language preference for $playerId",
                    exception
                )
            }
        }

    private fun delete(connection: Connection, playerId: UUID) {
        connection.prepareStatement(
            /* language=SQL */ """
            DELETE FROM
                ${LocalizationSqlSchema.PLAYER_LANGUAGES}
            WHERE
                namespace = ? AND player_uuid = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, playerId.toString())
            statement.executeUpdate()
        }
    }

    private fun readAll(connection: Connection): Map<UUID, LanguageId> =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                player_uuid,
                language
            FROM
                ${LocalizationSqlSchema.PLAYER_LANGUAGES}
            WHERE
                namespace = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.executeQuery().use { rs ->
                buildMap {
                    while (rs.next()) {
                        val id = try {
                            UUID.fromString(rs.getString(1))
                        } catch (_: IllegalArgumentException) {
                            throw StorageException(
                                "language preferences contain an invalid player uuid"
                            )
                        }
                        put(id, parseLanguage(rs.getString(2)))
                    }
                }
            }
        }

    private fun parseLanguage(raw: String): LanguageId =
        LanguageId.parse(raw)
            ?: throw StorageException("language preferences contain an invalid language id")

}
