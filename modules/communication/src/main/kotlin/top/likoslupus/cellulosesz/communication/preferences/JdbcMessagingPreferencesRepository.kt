package top.likoslupus.cellulosesz.communication.preferences

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.communication.CommunicationSqlSchema
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.sql.SQLException
import java.sql.Types
import java.util.*

internal class JdbcMessagingPreferencesRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : MessagingPreferencesRepository {

    override suspend fun load(playerId: UUID): MessagingPreferences? =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    val header = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            receive_private_messages,
                            reply_mode
                        FROM
                            ${CommunicationSqlSchema.MESSAGING_PREFERENCES}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeQuery().use { rs ->
                            when {
                                rs.next() -> rs.getBoolean(1) to rs.getString(2)
                                else -> null
                            }
                        }
                    } ?: return@read null

                    val ignored = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            ignored_uuid
                        FROM
                            ${CommunicationSqlSchema.IGNORED_PLAYERS}
                        WHERE
                            namespace = ? AND owner_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    val raw = rs.getString(1)
                                    add(parseUuid(raw, "ignored player"))
                                }
                            }
                        }
                    }

                    if (ignored.size > CommunicationSqlSchema.MAX_STORED_IGNORES) {
                        throw MessagingPreferencesDataException("stored ignore list exceeds the cap")
                    }

                    MessagingPreferences(
                        receivePrivateMessages = header.first,
                        ignoredPlayerIds = ignored.filter { it != playerId }.toSet(),
                        replyMode = parseReplyMode(header.second),
                    )
                }
            } catch (exception: SQLException) {
                throw MessagingPreferencesDataException(
                    "unable to read preferences for $playerId",
                    exception
                )
            }
        }

    override suspend fun save(
        playerId: UUID,
        playerName: String,
        preferences: MessagingPreferences,
    ) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                    DELETE FROM
                        ${CommunicationSqlSchema.MESSAGING_PREFERENCES}
                    WHERE
                        namespace = ? AND player_uuid = ?;
                    """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                    INSERT INTO ${CommunicationSqlSchema.MESSAGING_PREFERENCES} (
                        namespace,
                        player_uuid,
                        player_name,
                        receive_private_messages,
                        reply_mode
                    )
                    VALUES (
                        ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.setString(3, playerName)
                        statement.setBoolean(4, preferences.receivePrivateMessages)
                        when (preferences.replyMode) {
                            null -> statement.setNull(5, Types.VARCHAR)
                            else -> statement.setString(5, preferences.replyMode.name)
                        }
                        statement.executeUpdate()
                    }

                    connection.prepareStatement(
                        /* language=SQL */ """
                    DELETE FROM
                        ${CommunicationSqlSchema.IGNORED_PLAYERS}
                    WHERE
                        namespace = ? AND owner_uuid = ?;
                    """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeUpdate()
                    }
                    if (preferences.ignoredPlayerIds.isNotEmpty()) {
                        connection.prepareStatement(
                            /* language=SQL */ """
                        INSERT INTO ${CommunicationSqlSchema.IGNORED_PLAYERS} (
                            namespace,
                            owner_uuid,
                            ignored_uuid
                        )
                        VALUES (
                            ?, ?, ?
                        );
                        """.trimIndent()
                        ).use { statement ->
                            preferences.ignoredPlayerIds.forEach { ignored ->
                                statement.setString(1, namespace)
                                statement.setString(2, playerId.toString())
                                statement.setString(3, ignored.toString())
                                statement.addBatch()
                            }
                            statement.executeBatch()
                        }
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw MessagingPreferencesDataException(
                    "unable to write preferences for $playerId",
                    exception
                )
            }
        }

    override suspend fun delete(playerId: UUID): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${CommunicationSqlSchema.IGNORED_PLAYERS}
                        WHERE
                            namespace = ? AND owner_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${CommunicationSqlSchema.MESSAGING_PREFERENCES}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw MessagingPreferencesDataException(
                    "unable to delete preferences for $playerId",
                    exception
                )
            }
        }

    private fun parseReplyMode(raw: String?): ReplyMode? =
        raw?.let { value ->
            ReplyMode.entries.firstOrNull { it.name == value }
                ?: throw MessagingPreferencesDataException("unknown reply mode '$value'")
        }

    private fun parseUuid(raw: String, label: String): UUID =
        try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            throw MessagingPreferencesDataException("invalid $label UUID '$raw'")
        }

}
