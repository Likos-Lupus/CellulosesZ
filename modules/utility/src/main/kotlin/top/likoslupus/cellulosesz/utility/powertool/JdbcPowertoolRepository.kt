package top.likoslupus.cellulosesz.utility.powertool

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.foundation.parseUuidOrThrow
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.*

/** Powertool bindings in `cz_powertools`. */
internal class JdbcPowertoolRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : PowertoolRepository {

    override suspend fun loadAll(): Map<UUID, List<PowertoolBinding>> =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        item_id,
                        trigger,
                        command
                    FROM
                        ${UtilitySqlSchema.POWERTOOLS}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        val result = LinkedHashMap<UUID, MutableList<PowertoolBinding>>()
                        while (rs.next()) {
                            val playerId = parseUuid(rs.getString(1))
                            result.getOrPut(playerId) { mutableListOf() } += PowertoolBinding(
                                itemId = rs.getString(2),
                                trigger = parseTrigger(rs.getString(3)),
                                command = rs.getString(4),
                            )
                        }
                        result
                    }
                }
            }
        } catch (exception: SQLException) {
            throw PowertoolDataException("unable to read powertools", exception)
        }

    override suspend fun bind(
        playerId: UUID,
        binding: PowertoolBinding,
        at: Instant,
    ): Boolean =
        try {
            database.transactionIo { connection ->
                delete(connection, playerId, binding.itemId, binding.trigger)
                insert(connection, playerId, binding, at)
                true
            }
        } catch (exception: SQLException) {
            throw PowertoolDataException("unable to store powertool for $playerId", exception)
        }

    override suspend fun removeAll(playerId: UUID, itemId: String): Int =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${UtilitySqlSchema.POWERTOOLS}
                    WHERE
                        namespace = ? AND player_uuid = ? AND item_id = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, playerId.toString())
                    statement.setString(3, itemId)
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw PowertoolDataException("unable to remove powertool for $playerId", exception)
        }

    private fun delete(
        connection: Connection,
        playerId: UUID,
        itemId: String,
        trigger: PowertoolTrigger,
    ) {
        connection.prepareStatement(
            /* language=SQL */ """
            DELETE FROM
                ${UtilitySqlSchema.POWERTOOLS}
            WHERE
                namespace = ? AND player_uuid = ? AND item_id = ? AND trigger = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, playerId.toString())
            statement.setString(3, itemId)
            statement.setString(4, trigger.name)
            statement.executeUpdate()
        }
    }

    private fun insert(
        connection: Connection,
        playerId: UUID,
        binding: PowertoolBinding,
        at: Instant,
    ) {
        connection.prepareStatement(
            /* language=SQL */ """
            INSERT INTO ${UtilitySqlSchema.POWERTOOLS} (
                namespace,
                player_uuid,
                item_id,
                trigger,
                command,
                updated_at_ms
            )
            VALUES (
                ?, ?, ?, ?, ?, ?
            );
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, playerId.toString())
            statement.setString(3, binding.itemId)
            statement.setString(4, binding.trigger.name)
            statement.setString(5, binding.command)
            statement.setLong(6, at.toEpochMilli())
            statement.executeUpdate()
        }
    }

    private fun parseTrigger(raw: String): PowertoolTrigger =
        PowertoolTrigger.entries.firstOrNull { it.name == raw }
            ?: throw PowertoolDataException("powertools contain an unknown trigger '$raw'")

    private fun parseUuid(raw: String): UUID =
        parseUuidOrThrow(raw) {
            throw PowertoolDataException("powertools contain an invalid player id")
        }

}
