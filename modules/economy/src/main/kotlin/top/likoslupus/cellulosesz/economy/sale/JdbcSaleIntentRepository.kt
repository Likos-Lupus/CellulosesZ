package top.likoslupus.cellulosesz.economy.sale

import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.economy.ledger.EconomyDataException
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.util.*

/**
 * The JDBC owner of the durable sale intents. Transitions are guarded by the expected current state
 * so a replayed or out-of-order write is a no-op instead of a regression.
 */
internal class JdbcSaleIntentRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : SaleIntentRepository {

    override suspend fun insert(record: SaleIntentRecord): Unit =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${EconomySqlSchema.SALE_INTENTS} (
                        namespace,
                        sale_id,
                        player_uuid,
                        player_name,
                        state,
                        item_id,
                        item_count,
                        total_minor,
                        payload,
                        transaction_id,
                        created_at_ms,
                        updated_at_ms
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, record.saleId.toString())
                    statement.setString(3, record.playerId.toString())
                    statement.setString(4, record.playerName)
                    statement.setString(5, record.state.name)
                    statement.setString(6, record.itemId)
                    statement.setInt(7, record.itemCount)
                    statement.setLong(8, record.totalMinor)
                    statement.setString(9, record.payload)
                    when (record.transactionId) {
                        null -> statement.setNull(10, Types.VARCHAR)
                        else -> statement.setString(10, record.transactionId.toString())
                    }
                    statement.setLong(11, record.createdAtMs)
                    statement.setLong(12, record.updatedAtMs)
                    statement.executeUpdate()
                }
            }
            Unit
        } catch (exception: SQLException) {
            throw EconomyDataException("unable to record sale '${record.saleId}'", exception)
        }

    override suspend fun markTaken(saleId: UUID, at: Instant): Boolean =
        transition(
            sql = /* language=SQL */ """
            UPDATE
                ${EconomySqlSchema.SALE_INTENTS}
            SET
                state = ?,
                updated_at_ms = ?
            WHERE
                namespace = ?
                AND sale_id = ?
                AND state = ?;
            """.trimIndent(),
        ) { statement ->
            statement.setString(1, SaleIntentState.TAKEN.name)
            statement.setLong(2, at.toEpochMilli())
            statement.setString(3, namespace)
            statement.setString(4, saleId.toString())
            statement.setString(5, SaleIntentState.PREPARED.name)
        }

    override suspend fun markCommitted(
        saleId: UUID,
        transactionId: UUID,
        at: Instant,
    ): Boolean =
        transition(
            sql = /* language=SQL */ """
            UPDATE
                ${EconomySqlSchema.SALE_INTENTS}
            SET
                state = ?,
                transaction_id = ?,
                updated_at_ms = ?
            WHERE
                namespace = ?
                AND sale_id = ?
                AND state = ?;
            """.trimIndent(),
        ) { statement ->
            statement.setString(1, SaleIntentState.COMMITTED.name)
            statement.setString(2, transactionId.toString())
            statement.setLong(3, at.toEpochMilli())
            statement.setString(4, namespace)
            statement.setString(5, saleId.toString())
            statement.setString(6, SaleIntentState.TAKEN.name)
        }

    override suspend fun markRolledBack(saleId: UUID, at: Instant): Boolean =
        transition(
            sql = /* language=SQL */ """
            UPDATE
                ${EconomySqlSchema.SALE_INTENTS}
            SET
                state = ?,
                updated_at_ms = ?
            WHERE
                namespace = ?
                AND sale_id = ?
                AND state IN (?, ?);
            """.trimIndent(),
        ) { statement ->
            statement.setString(1, SaleIntentState.ROLLED_BACK.name)
            statement.setLong(2, at.toEpochMilli())
            statement.setString(3, namespace)
            statement.setString(4, saleId.toString())
            statement.setString(5, SaleIntentState.PREPARED.name)
            statement.setString(6, SaleIntentState.TAKEN.name)
        }

    override suspend fun loadOpen(): List<SaleIntentRecord> =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        sale_id,
                        player_uuid,
                        player_name,
                        state,
                        item_id,
                        item_count,
                        total_minor,
                        payload,
                        transaction_id,
                        created_at_ms,
                        updated_at_ms
                    FROM
                        ${EconomySqlSchema.SALE_INTENTS}
                    WHERE
                        namespace = ?
                        AND state IN (?, ?)
                    ORDER BY
                        created_at_ms ASC, sale_id ASC;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, SaleIntentState.PREPARED.name)
                    statement.setString(3, SaleIntentState.TAKEN.name)
                    statement.executeQuery().use(::readRows)
                }
            }
        } catch (exception: SQLException) {
            throw EconomyDataException("unable to load open sale records", exception)
        }

    private suspend fun transition(
        sql: String,
        bind: (PreparedStatement) -> Unit,
    ): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(sql).use { statement ->
                    bind(statement)
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw EconomyDataException("unable to update a sale record", exception)
        }

    private fun readRows(rs: ResultSet): List<SaleIntentRecord> =
        buildList {
            while (rs.next()) {
                add(
                    SaleIntentRecord(
                        saleId = UUID.fromString(rs.getString(1)),
                        playerId = UUID.fromString(rs.getString(2)),
                        playerName = rs.getString(3),
                        state = parseState(rs.getString(4)),
                        itemId = rs.getString(5),
                        itemCount = rs.getInt(6),
                        totalMinor = rs.getLong(7),
                        payload = rs.getString(8),
                        transactionId = rs.getString(9)?.let(UUID::fromString),
                        createdAtMs = rs.getLong(10),
                        updatedAtMs = rs.getLong(11),
                    )
                )
            }
        }

    private fun parseState(raw: String): SaleIntentState =
        SaleIntentState.entries.firstOrNull { it.name == raw }
            ?: throw EconomyDataException("unknown sale intent state '$raw'")

}
