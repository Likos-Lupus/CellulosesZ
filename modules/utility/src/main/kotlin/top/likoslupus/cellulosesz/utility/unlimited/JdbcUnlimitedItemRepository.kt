package top.likoslupus.cellulosesz.utility.unlimited

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.foundation.parseUuidOrThrow
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import java.sql.Connection
import java.sql.SQLException
import java.util.*

/** Per-player unlimited item sets in `cz_unlimited_items`. */
internal class JdbcUnlimitedItemRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : UnlimitedItemRepository {

    override suspend fun loadAll(): Map<UUID, Set<String>> =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        item_id
                    FROM
                        ${UtilitySqlSchema.UNLIMITED_ITEMS}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        val result = LinkedHashMap<UUID, MutableSet<String>>()
                        while (rs.next()) {
                            val playerId = parseUuid(rs.getString(1))
                            result.getOrPut(playerId) { LinkedHashSet() } += rs.getString(2)
                        }
                        result
                    }
                }
            }
        } catch (exception: SQLException) {
            throw UnlimitedItemDataException("unable to read unlimited items", exception)
        }

    override suspend fun add(playerId: UUID, itemId: String): Boolean =
        try {
            database.transactionIo { connection ->
                if (exists(connection, playerId, itemId)) {
                    false
                } else {
                    insert(connection, playerId, itemId)
                    true
                }
            }
        } catch (exception: SQLException) {
            throw UnlimitedItemDataException(
                "unable to record unlimited item for $playerId",
                exception,
            )
        }

    override suspend fun remove(playerId: UUID, itemId: String): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${UtilitySqlSchema.UNLIMITED_ITEMS}
                    WHERE
                        namespace = ? AND player_uuid = ? AND item_id = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, playerId.toString())
                    statement.setString(3, itemId)
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw UnlimitedItemDataException(
                "unable to remove unlimited item for $playerId",
                exception,
            )
        }

    override suspend fun clear(playerId: UUID): Int =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${UtilitySqlSchema.UNLIMITED_ITEMS}
                    WHERE
                        namespace = ? AND player_uuid = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, playerId.toString())
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw UnlimitedItemDataException(
                "unable to clear unlimited items for $playerId",
                exception,
            )
        }

    private fun exists(connection: Connection, playerId: UUID, itemId: String): Boolean =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                1
            FROM
                ${UtilitySqlSchema.UNLIMITED_ITEMS}
            WHERE
                namespace = ? AND player_uuid = ? AND item_id = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, playerId.toString())
            statement.setString(3, itemId)
            statement.executeQuery().use { rs -> rs.next() }
        }

    private fun insert(connection: Connection, playerId: UUID, itemId: String) {
        connection.prepareStatement(
            /* language=SQL */ """
            INSERT INTO ${UtilitySqlSchema.UNLIMITED_ITEMS} (
                namespace,
                player_uuid,
                item_id
            )
            VALUES (
                ?, ?, ?
            );
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, playerId.toString())
            statement.setString(3, itemId)
            statement.executeUpdate()
        }
    }

    private fun parseUuid(raw: String): UUID =
        parseUuidOrThrow(raw) {
            throw UnlimitedItemDataException("unlimited items contain an invalid player id")
        }

}
