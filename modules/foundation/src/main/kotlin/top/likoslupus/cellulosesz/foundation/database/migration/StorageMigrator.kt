package top.likoslupus.cellulosesz.foundation.database.migration

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.ResolvedDatabaseDescriptor
import top.likoslupus.cellulosesz.foundation.database.StorageException
import top.likoslupus.cellulosesz.foundation.database.schema.*
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.*

public sealed interface StorageMigrationResult {

    public data object AlreadyCompleted : StorageMigrationResult

    public data class Completed(
        public val tables: Int,
        public val rows: Long,
    ) : StorageMigrationResult

}

/**
 * Copies every business table from a previous successful endpoint into the freshly initialized
 * target, inside a single target transaction that also records a [MigrationRecord]. The source is
 * opened read-only and is never modified or deleted. A non-empty target without a matching journal
 * record is refused rather than merged.
 */
public object StorageMigrator {

    private const val BATCH = 500

    public fun migrateBlocking(
        source: ResolvedDatabaseDescriptor,
        target: DatabaseRuntime,
        contributors: List<SqlSchemaContributor>,
        sourceNamespace: String,
        targetNamespace: String,
    ): StorageMigrationResult {
        val dialect = SqlDialects.of(target.type)
        val journal = MigrationJournal(dialect)
        val sourceHash = source.identity.hash()
        val targetHash = target.identity.hash()

        if (
            journal.completed(
                target,
                sourceHash,
                targetHash
            ) != null
        ) {
            return StorageMigrationResult.AlreadyCompleted
        }

        val tables = contributors.flatMap { it.tables() }
        guardTargetEmpty(target, dialect, tables, targetNamespace)

        var totalRows = 0L
        HikariDatabaseRuntime.create(source).use {
            target.transaction { targetConnection ->
                it.read { sourceConnection ->
                    tables.forEach { table ->
                        val sourceCount = countRows(
                            sourceConnection,
                            dialect,
                            table,
                            sourceNamespace
                        )
                        val copied = copyTable(
                            source = sourceConnection,
                            target = targetConnection,
                            dialect = dialect,
                            table = table,
                            sourceNamespace = sourceNamespace,
                            targetNamespace = targetNamespace,
                        )
                        if (copied.toLong() != sourceCount) {
                            throw StorageException(
                                "migration row count mismatch for ${table.name}: copied $copied of $sourceCount"
                            )
                        }
                        totalRows += copied
                    }
                }
                journal.markCompleted(
                    targetConnection,
                    MigrationRecord(
                        migrationId = UUID.randomUUID().toString(),
                        namespace = targetNamespace,
                        sourceIdentityHash = sourceHash,
                        targetIdentityHash = targetHash,
                        completedAtEpochMs = System.currentTimeMillis(),
                    ),
                )
            }
        }
        return StorageMigrationResult.Completed(tables.size, totalRows)
    }

    private fun guardTargetEmpty(
        target: DatabaseRuntime,
        dialect: SqlDialect,
        tables: List<SqlTable>,
        namespace: String,
    ) {
        target.read {
            tables.forEach { table ->
                val count = countRows(it, dialect, table, namespace)
                if (count > 0) {
                    throw StorageException(
                        "target storage already holds ${table.name}=$count rows and has no matching " +
                                "migration record; refusing to merge automatically"
                    )
                }
            }
        }
    }

    private fun countRows(
        connection: Connection,
        dialect: SqlDialect,
        table: SqlTable,
        namespace: String,
    ): Long {
        val namespaceColumn = namespaceColumnOrNull(table)
        val sql = if (namespaceColumn != null) {
            /* language=SQL */ """
            SELECT
                COUNT(*)
            FROM
                ${dialect.quote(table.name)}
            WHERE
                ${dialect.quote(namespaceColumn)} = ?;
            """.trimIndent()
        } else {
            /* language=SQL */ """
            SELECT
                COUNT(*)
            FROM
                ${dialect.quote(table.name)};
            """.trimIndent()
        }

        connection.prepareStatement(sql).use {
            if (namespaceColumn != null) {
                it.setString(1, namespace)
            }
            it.executeQuery().use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }

    private fun copyTable(
        source: Connection,
        target: Connection,
        dialect: SqlDialect,
        table: SqlTable,
        sourceNamespace: String,
        targetNamespace: String,
    ): Int {
        val columns = table.columns
        val namespaceColumn = namespaceColumnOrNull(table)
        val selectSql = /* language=SQL */ """
            SELECT
                ${columns.joinToString(", ") { dialect.quote(it.name) }}
            FROM
                ${dialect.quote(table.name)}
            ${if (namespaceColumn != null) "WHERE ${dialect.quote(namespaceColumn)} = ?" else ""};
            """.trimIndent()
        val insertSql = /* language=SQL */ """
            INSERT INTO ${dialect.quote(table.name)} (
                ${columns.joinToString(", ") { dialect.quote(it.name) }}
            )
            VALUES (
                ${columns.joinToString(", ") { "?" }}
            );
            """.trimIndent()

        source.prepareStatement(selectSql).use { select ->
            select.fetchSize = BATCH
            if (namespaceColumn != null) {
                select.setString(1, sourceNamespace)
            }
            select.executeQuery().use { rs ->
                target.prepareStatement(insertSql).use { insert ->
                    var copied = 0
                    var pending = 0
                    while (rs.next()) {
                        bindRow(insert, columns, rs, namespaceColumn, targetNamespace)
                        insert.addBatch()
                        copied++
                        if (++pending >= BATCH) {
                            insert.executeBatch()
                            pending = 0
                        }
                    }
                    if (pending > 0) {
                        insert.executeBatch()
                    }
                    return copied
                }
            }
        }
    }

    private fun bindRow(
        statement: PreparedStatement,
        columns: List<SqlColumn>,
        rs: ResultSet,
        namespaceColumn: String?,
        targetNamespace: String,
    ) {
        columns.forEachIndexed { index, column ->
            val position = index + 1
            if (column.name == namespaceColumn) {
                statement.setString(position, targetNamespace)
                return@forEachIndexed
            }

            when (column.type) {
                SqlColumnType.UUID,
                SqlColumnType.NAME,
                SqlColumnType.RESOURCE,
                SqlColumnType.HASH,
                SqlColumnType.TEXT ->
                    when (val value = rs.getString(position)) {
                        null -> statement.setNull(
                            position,
                            Types.VARCHAR
                        )

                        else -> statement.setString(
                            position,
                            value
                        )
                    }

                SqlColumnType.BIGINT ->
                    when (rs.getObject(position)) {
                        null -> statement.setNull(
                            position,
                            Types.BIGINT
                        )

                        else -> statement.setLong(
                            position,
                            rs.getLong(position)
                        )
                    }

                SqlColumnType.INTEGER ->
                    when (rs.getObject(position)) {
                        null -> statement.setNull(
                            position,
                            Types.INTEGER
                        )

                        else -> statement.setInt(
                            position,
                            rs.getInt(position)
                        )
                    }

                SqlColumnType.DOUBLE,
                SqlColumnType.REAL ->
                    when (rs.getObject(position)) {
                        null -> statement.setNull(
                            position,
                            Types.DOUBLE
                        )

                        else -> statement.setDouble(
                            position,
                            rs.getDouble(position)
                        )
                    }

                SqlColumnType.BOOLEAN ->
                    when (rs.getObject(position)) {
                        null -> statement.setNull(
                            position,
                            Types.BOOLEAN
                        )

                        else -> statement.setBoolean(
                            position,
                            rs.getBoolean(position)
                        )
                    }
            }
        }
    }

    private fun namespaceColumnOrNull(table: SqlTable): String? =
        table.columns.firstOrNull { it.name == "namespace" }?.name

}
