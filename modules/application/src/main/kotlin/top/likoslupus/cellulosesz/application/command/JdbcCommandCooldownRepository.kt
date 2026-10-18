package top.likoslupus.cellulosesz.application.command

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.application.ApplicationSqlSchema
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import java.sql.SQLException
import java.util.*

/** The JDBC owner of `cz_command_cooldowns`; the cache in [CommandCooldownStore] never sees SQL. */
internal class JdbcCommandCooldownRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) {

    private val logger = LoggerFactory.getLogger(JdbcCommandCooldownRepository::class.java)

    fun loadAllBlocking(now: Long): Map<UUID, Map<String, Long>> =
        try {
            database.read { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        command,
                        expires_at_ms
                    FROM
                        ${ApplicationSqlSchema.COMMAND_COOLDOWNS}
                    WHERE
                        namespace = ? AND expires_at_ms > ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setLong(2, now)
                    statement.executeQuery().use { rs ->
                        val loaded = HashMap<UUID, MutableMap<String, Long>>()
                        while (rs.next()) {
                            val playerId = runCatching {
                                UUID.fromString(rs.getString(1))
                            }.getOrNull() ?: continue
                            loaded.computeIfAbsent(playerId) { HashMap() }[rs.getString(2)] =
                                rs.getLong(3)
                        }
                        loaded.mapValues { (_, values) -> values.toMap() }
                    }
                }
            }
        } catch (exception: SQLException) {
            logger.warn("command_cooldown_hydrate_failed", exception)
            emptyMap()
        }

    suspend fun upsert(
        playerId: UUID,
        command: String,
        expiresAt: Long,
    ) {
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${ApplicationSqlSchema.COMMAND_COOLDOWNS}
                    WHERE
                        namespace = ? AND player_uuid = ? AND command = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, playerId.toString())
                    statement.setString(3, command)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${ApplicationSqlSchema.COMMAND_COOLDOWNS} (
                        namespace,
                        player_uuid,
                        command,
                        expires_at_ms
                    )
                    VALUES (
                        ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, playerId.toString())
                    statement.setString(3, command)
                    statement.setLong(4, expiresAt)
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            logger.warn(
                "command_cooldown_write_failed player={} command={}",
                playerId,
                command,
                exception,
            )
        }
    }

}
