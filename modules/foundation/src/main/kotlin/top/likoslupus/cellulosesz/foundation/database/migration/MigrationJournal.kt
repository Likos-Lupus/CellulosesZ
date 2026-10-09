package top.likoslupus.cellulosesz.foundation.database.migration

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.schema.*
import java.sql.Connection

/** A completed-migration marker written in the *target* transaction that copied the rows. */
public data class MigrationRecord(
    public val migrationId: String,
    public val namespace: String,
    public val sourceIdentityHash: String,
    public val targetIdentityHash: String,
    public val completedAtEpochMs: Long,
)

/**
 * A tiny crash-recovery journal (not a schema version). Its presence proves that a copy committed,
 * so a crash between the target commit and the sidecar rewrite can be finalized without recopying.
 */
public object MigrationJournalSchema : SqlSchemaContributor {

    public const val TABLE: String = "cz_storage_migration_journal"

    override val id: String
        get() = "foundation.migration-journal"

    override fun tables(): List<SqlTable> = listOf(
        SqlTable(
            name = TABLE,
            columns = listOf(
                SqlColumn("migration_id", SqlColumnType.UUID),
                SqlColumn("namespace", SqlColumnType.NAME),
                SqlColumn("source_identity_hash", SqlColumnType.HASH),
                SqlColumn("target_identity_hash", SqlColumnType.HASH),
                SqlColumn("completed_at_ms", SqlColumnType.BIGINT),
            ),
            primaryKey = listOf("migration_id"),
        )
    )

    override fun indexes(): List<SqlIndex> =
        emptyList()

}

public class MigrationJournal(
    private val dialect: SqlDialect
) {

    /** Returns the most recent completed record matching source+target, if any. */
    public fun completed(
        runtime: DatabaseRuntime,
        sourceIdentityHash: String,
        targetIdentityHash: String,
    ): MigrationRecord? =
        runtime.read {
            val sql = /* language=SQL */ """
                SELECT
                    migration_id,
                    namespace,
                    source_identity_hash,
                    target_identity_hash,
                    completed_at_ms
                FROM
                    ${dialect.quote(MigrationJournalSchema.TABLE)}
                WHERE
                    ${dialect.quote("source_identity_hash")} = ?
                    AND ${dialect.quote("target_identity_hash")} = ?;
                """.trimIndent()
            it.prepareStatement(sql).use { statement ->
                statement.setString(1, sourceIdentityHash)
                statement.setString(2, targetIdentityHash)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        MigrationRecord(
                            migrationId = rs.getString(1),
                            namespace = rs.getString(2),
                            sourceIdentityHash = rs.getString(3),
                            targetIdentityHash = rs.getString(4),
                            completedAtEpochMs = rs.getLong(5),
                        )
                    } else {
                        null
                    }
                }
            }
        }

    public fun markCompleted(
        connection: Connection,
        record: MigrationRecord
    ) {
        val columns = listOf(
            "migration_id",
            "namespace",
            "source_identity_hash",
            "target_identity_hash",
            "completed_at_ms",
        )
        val sql = dialect.upsertSql(
            MigrationJournalSchema.TABLE,
            listOf("migration_id"),
            columns
        )
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, record.migrationId)
            statement.setString(2, record.namespace)
            statement.setString(3, record.sourceIdentityHash)
            statement.setString(4, record.targetIdentityHash)
            statement.setLong(5, record.completedAtEpochMs)
            statement.executeUpdate()
        }
    }

}
