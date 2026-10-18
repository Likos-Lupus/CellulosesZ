package top.likoslupus.cellulosesz.utility.kit

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.*

/** The system-grant ledger in `cz_kit_grants`; RESERVED is written before delivery (fail closed). */
internal class JdbcKitGrantRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : KitGrantRepository {

    override suspend fun find(
        addonId: String,
        grantId: String,
        playerId: UUID,
    ): KitGrantRecord? =
        try {
            database.readIo { connection ->
                find(connection, addonId, grantId, playerId)
            }
        } catch (exception: SQLException) {
            throw KitGrantDataException("unable to read kit grant '$grantId'", exception)
        }

    override suspend fun reserve(
        addonId: String,
        playerId: UUID,
        kitName: KitName,
        grantId: String,
        at: Instant,
    ): KitGrantReservation =
        try {
            database.transactionIo { connection ->
                val existing = find(connection, addonId, grantId, playerId)
                if (existing != null) {
                    KitGrantReservation.Existing(existing.state)
                } else {
                    insert(connection, addonId, playerId, kitName, grantId, at)
                    KitGrantReservation.Inserted
                }
            }
        } catch (exception: SQLException) {
            // A concurrent insert against the unique key is not an infrastructure failure:
            // re-read the now-existing row instead of reporting storage as unavailable.
            val existing = runCatching {
                database.readIo { connection ->
                    find(connection, addonId, grantId, playerId)
                }
            }.getOrNull()
            when {
                existing != null -> KitGrantReservation.Existing(existing.state)
                else -> throw KitGrantDataException(
                    "unable to reserve kit grant '$grantId'",
                    exception,
                )
            }
        }

    override suspend fun markDelivered(
        addonId: String,
        grantId: String,
        playerId: UUID,
        at: Instant,
    ): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    UPDATE
                        ${UtilitySqlSchema.KIT_GRANTS}
                    SET
                        state = ?,
                        delivered_at_ms = ?
                    WHERE
                        namespace = ?
                        AND addon_id = ?
                        AND grant_id = ?
                        AND player_uuid = ?
                        AND state = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, KitGrantStatus.DELIVERED.name)
                    statement.setLong(2, at.toEpochMilli())
                    statement.setString(3, namespace)
                    statement.setString(4, addonId)
                    statement.setString(5, grantId)
                    statement.setString(6, playerId.toString())
                    statement.setString(7, KitGrantStatus.RESERVED.name)
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw KitGrantDataException("unable to finalize kit grant '$grantId'", exception)
        }

    private fun insert(
        connection: Connection,
        addonId: String,
        playerId: UUID,
        kitName: KitName,
        grantId: String,
        at: Instant,
    ) {
        connection.prepareStatement(
            /* language=SQL */ """
            INSERT INTO ${UtilitySqlSchema.KIT_GRANTS} (
                namespace,
                addon_id,
                grant_id,
                player_uuid,
                kit_name,
                state,
                created_at_ms,
                delivered_at_ms
            )
            VALUES (
                ?, ?, ?, ?, ?, ?, ?, NULL
            );
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, addonId)
            statement.setString(3, grantId)
            statement.setString(4, playerId.toString())
            statement.setString(5, kitName.value)
            statement.setString(6, KitGrantStatus.RESERVED.name)
            statement.setLong(7, at.toEpochMilli())
            statement.executeUpdate()
        }
    }

    private fun find(
        connection: Connection,
        addonId: String,
        grantId: String,
        playerId: UUID,
    ): KitGrantRecord? =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                kit_name,
                state,
                created_at_ms,
                delivered_at_ms
            FROM
                ${UtilitySqlSchema.KIT_GRANTS}
            WHERE
                namespace = ? AND addon_id = ? AND grant_id = ? AND player_uuid = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, addonId)
            statement.setString(3, grantId)
            statement.setString(4, playerId.toString())
            statement.executeQuery().use { rs ->
                if (!rs.next()) {
                    return null
                }
                val name = KitName.parse(rs.getString(1))
                    ?: throw KitGrantDataException("kit grant contains an invalid kit name")
                KitGrantRecord(
                    addonId = addonId,
                    grantId = grantId,
                    playerId = playerId,
                    kitName = name,
                    state = parseState(rs.getString(2)),
                    createdAt = Instant.ofEpochMilli(rs.getLong(3)),
                    deliveredAt = rs.getObject(4)?.let {
                        Instant.ofEpochMilli(rs.getLong(4))
                    },
                )
            }
        }

    private fun parseState(raw: String): KitGrantStatus =
        KitGrantStatus.entries.firstOrNull { it.name == raw }
            ?: throw KitGrantDataException("kit grant contains an unknown state '$raw'")

}
