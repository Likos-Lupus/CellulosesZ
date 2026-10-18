package top.likoslupus.cellulosesz.economy.worth

import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.economy.ledger.EconomyDataException
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.schema.SqlDialects
import top.likoslupus.cellulosesz.foundation.database.transactionIo
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

    /**
     * Resolved per write, not cached in the constructor: feature repositories are built during mod
     * init, while the `DeferredDatabaseRuntime` is bound at `SERVER_STARTING`. Caching would also
     * pin the dialect of a previous integrated-server session after a backend change.
     */
    private fun upsertSql(): String =
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

    override suspend fun insertMissing(records: List<WorthRecord>) {
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
                        bindWorthRecord(it, record)
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

    @Suppress("SqlSourceToSinkFlow") // SQL is generated from constant table/column names.
    private fun writeRecord(connection: Connection, record: WorthRecord) {
        connection.prepareStatement(upsertSql()).use { statement ->
            bindWorthRecord(statement, record)
            statement.executeUpdate()
        }
    }

    private fun bindWorthRecord(statement: PreparedStatement, record: WorthRecord) {
        statement.setString(1, namespace)
        statement.setString(2, record.itemId)
        statement.setLong(3, record.priceMinor)
        statement.setLong(4, record.updatedAt.toEpochMilli())
        setUpdatedBy(statement, record.updatedBy)
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

    private fun setUpdatedBy(statement: PreparedStatement, value: String?) {
        when (value) {
            null -> statement.setNull(5, Types.VARCHAR)
            else -> statement.setString(5, value)
        }
    }

}
