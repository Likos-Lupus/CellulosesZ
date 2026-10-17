package top.likoslupus.cellulosesz.foundation.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.migration.MigrationJournalSchema
import top.likoslupus.cellulosesz.foundation.database.migration.StorageMigrationResult
import top.likoslupus.cellulosesz.foundation.database.migration.StorageMigrator
import top.likoslupus.cellulosesz.foundation.database.schema.*
import java.nio.file.Path

/**
 * The economy tables are introduced as one all-or-nothing schema group. A source that predates the
 * whole group (all absent) is a legacy source with no economy data; a source with every table is
 * migrated normally; and a source with only some of the group is a broken source that must be
 * refused rather than silently migrated.
 */
class EconomySchemaGroupMigrationTest {

    @TempDir
    lateinit var directory: Path

    private object BaseSchema : SqlSchemaContributor {

        const val TABLE = "cz_test_base"
        override val id: String get() = "base"

        override fun tables(): List<SqlTable> = listOf(
            SqlTable(
                TABLE,
                listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("row_id", SqlColumnType.NAME),
                ),
                listOf("namespace", "row_id"),
            )
        )

        override fun indexes(): List<SqlIndex> = emptyList()
    }

    private class EconomyTableSchema(
        private val id0: String,
        private val tableName: String
    ) : SqlSchemaContributor {

        override val id: String get() = id0

        override fun tables(): List<SqlTable> = listOf(
            SqlTable(
                tableName,
                listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("row_id", SqlColumnType.NAME),
                ),
                listOf("namespace", "row_id"),
            )
        )

        override fun indexes(): List<SqlIndex> = emptyList()
    }

    private val accounts = EconomyTableSchema(
        "eco-accounts",
        "cz_economy_accounts"
    )
    private val transactions = EconomyTableSchema(
        "eco-transactions",
        "cz_economy_transactions"
    )
    private val entries = EconomyTableSchema(
        "eco-entries",
        "cz_economy_entries"
    )
    private val economyContributors = listOf(
        accounts,
        transactions,
        entries
    )

    private fun descriptor(path: String): ResolvedDatabaseDescriptor =
        DatabaseDescriptorResolver.resolve(
            DatabaseSettings(sqlite = SqliteSettings(path = path)),
            directory,
        )

    private fun insertRow(runtime: HikariDatabaseRuntime, table: String, id: String) {
        runtime.transaction { connection ->
            connection.prepareStatement(
                /* language=SQL */ "INSERT INTO $table (namespace, row_id) VALUES ('default', '$id');"
            ).use { it.executeUpdate() }
        }
    }

    @Test
    fun `a legacy source without any economy table is migrated as empty`() {
        val sourceDescriptor = descriptor("legacy-source.db")
        HikariDatabaseRuntime.create(sourceDescriptor).use { source ->
            SchemaInitializer.initializeBlocking(source, listOf(BaseSchema))
            insertRow(source, BaseSchema.TABLE, "base-1")
        }

        val targetDescriptor = descriptor("legacy-target")
        HikariDatabaseRuntime.create(targetDescriptor).use { target ->
            SchemaInitializer.initializeBlocking(
                target,
                listOf(MigrationJournalSchema, BaseSchema) + economyContributors,
            )

            val result = StorageMigrator.migrateBlocking(
                source = sourceDescriptor,
                target = target,
                contributors = listOf(BaseSchema) + economyContributors,
                sourceNamespace = "default",
                targetNamespace = "default",
            )

            assertEquals(
                StorageMigrationResult.Completed(4, 1),
                result
            )
            assertEquals(
                0,
                rowCount(target, "cz_economy_accounts")
            )
        }
    }

    @Test
    fun `a complete economy group is migrated in full`() {
        val sourceDescriptor = descriptor("full-source.db")
        HikariDatabaseRuntime.create(sourceDescriptor).use { source ->
            SchemaInitializer.initializeBlocking(
                source,
                listOf(BaseSchema) + economyContributors,
            )
            insertRow(source, BaseSchema.TABLE, "base-1")
            insertRow(source, "cz_economy_accounts", "a-1")
            insertRow(source, "cz_economy_transactions", "t-1")
            insertRow(source, "cz_economy_entries", "e-1")
        }

        val targetDescriptor = descriptor("full-target")
        HikariDatabaseRuntime.create(targetDescriptor).use { target ->
            SchemaInitializer.initializeBlocking(
                target,
                listOf(MigrationJournalSchema, BaseSchema) + economyContributors,
            )

            val result = StorageMigrator.migrateBlocking(
                source = sourceDescriptor,
                target = target,
                contributors = listOf(BaseSchema) + economyContributors,
                sourceNamespace = "default",
                targetNamespace = "default",
            )

            assertEquals(
                StorageMigrationResult.Completed(4, 4),
                result
            )
            assertEquals(
                1,
                rowCount(target, "cz_economy_accounts")
            )
            assertEquals(
                1,
                rowCount(target, "cz_economy_transactions")
            )
            assertEquals(
                1,
                rowCount(target, "cz_economy_entries")
            )
        }
    }

    @Test
    fun `a partially-present economy group refuses to migrate`() {
        val sourceDescriptor = descriptor("broken-source.db")
        HikariDatabaseRuntime.create(sourceDescriptor).use { source ->
            // Only one of the three economy tables exists: a broken source, not a legacy one.
            SchemaInitializer.initializeBlocking(
                source,
                listOf(BaseSchema, accounts)
            )
            insertRow(source, BaseSchema.TABLE, "base-1")
        }

        val targetDescriptor = descriptor("broken-target")
        HikariDatabaseRuntime.create(targetDescriptor).use { target ->
            SchemaInitializer.initializeBlocking(
                target,
                listOf(MigrationJournalSchema, BaseSchema) + economyContributors,
            )

            assertThrows(StorageException::class.java) {
                StorageMigrator.migrateBlocking(
                    source = sourceDescriptor,
                    target = target,
                    contributors = listOf(BaseSchema) + economyContributors,
                    sourceNamespace = "default",
                    targetNamespace = "default",
                )
            }
        }
    }

    private fun rowCount(runtime: DatabaseRuntime, table: String): Int =
        runtime.read { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            }
        }

}
