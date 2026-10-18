package top.likoslupus.cellulosesz.economy.worth

import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.economy.ledger.EconomyDataException
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.foundation.database.schema.SqlDialects
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Types
import java.time.Instant

/**
 * The JDBC owner of the worth catalog. Prices are BIGINT minor units; the item id is the logical
 * key. Every failure surfaces as a typed [EconomyDataException], never a silently dropped row.
 */
internal class JdbcWorthRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : WorthRepository {

    private val upsertSql: String =
        SqlDialects.of(database.type).upsertSql(
            table = EconomySqlSchema.WORTH,
            keyColumns = listOf("namespace", "item_id"),
            columns = listOf(
                "namespace",
                "item_id",
                "price_minor",
                "updated_at_ms",
                "updated_by",
            ),
        )

    override suspend fun loadAll(): List<WorthRecord> =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        item_id,
                        price_minor,
                        updated_at_ms,
                        updated_by
                    FROM
                        ${EconomySqlSchema.WORTH}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use {
                    it.setString(1, namespace)
                    it.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    WorthRecord(
                                        itemId = rs.getString(1),
                                        priceMinor = rs.getLong(2),
                                        updatedAt = Instant.ofEpochMilli(rs.getLong(3)),
                                        updatedBy = rs.getString(4),
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            throw EconomyDataException("unable to load the worth catalog", exception)
        }

    override suspend fun upsert(record: WorthRecord): Unit =
        try {
            database.transactionIo { connection ->
                writeRecord(connection, record)
            }
        } catch (exception: SQLException) {
            throw EconomyDataException(
                "unable to store the worth of '${record.itemId}'",
                exception
            )
        }

    override suspend fun insertMissing(records: List<WorthRecord>): Unit {
        if (records.isEmpty()) return
        try {
            database.transactionIo { connection ->
                records.forEach { record ->
                    if (exists(connection, record.itemId)) return@forEach
                    connection.prepareStatement(
                        /* language=SQL */ """
                            INSERT INTO ${EconomySqlSchema.WORTH} (
                                namespace,
                                item_id,
                                price_minor,
                                updated_at_ms,
                                updated_by
                            )
                            VALUES (?, ?, ?, ?, ?);
                            """.trimIndent()
                    ).use {
                        it.setString(1, namespace)
                        it.setString(2, record.itemId)
                        it.setLong(3, record.priceMinor)
                        it.setLong(4, record.updatedAt.toEpochMilli())
                        setUpdatedBy(it, 5, record.updatedBy)
                        it.executeUpdate()
                    }
                }
            }
        } catch (exception: SQLException) {
            throw EconomyDataException("unable to import the default worth catalog", exception)
        }
    }

    override suspend fun count(): Int =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        COUNT(*)
                    FROM
                        ${EconomySqlSchema.WORTH}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
            }
        } catch (exception: SQLException) {
            throw EconomyDataException("unable to count the worth catalog", exception)
        }

    private fun writeRecord(connection: Connection, record: WorthRecord) {
        connection.prepareStatement(upsertSql).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, record.itemId)
            statement.setLong(3, record.priceMinor)
            statement.setLong(4, record.updatedAt.toEpochMilli())
            setUpdatedBy(statement, 5, record.updatedBy)
            statement.executeUpdate()
        }
    }

    private fun exists(connection: Connection, itemId: String): Boolean =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                1
            FROM
                ${EconomySqlSchema.WORTH}
            WHERE
                namespace = ? AND item_id = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, itemId)
            statement.executeQuery().use { rs -> rs.next() }
        }

    private fun setUpdatedBy(
        statement: PreparedStatement,
        index: Int,
        value: String?,
    ) {
        when (value) {
            null -> statement.setNull(index, Types.VARCHAR)
            else -> statement.setString(index, value)
        }
    }

}
