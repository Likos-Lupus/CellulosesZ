package top.likoslupus.cellulosesz.foundation.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.migration.MigrationJournalSchema
import top.likoslupus.cellulosesz.foundation.database.migration.StorageMigrationResult
import top.likoslupus.cellulosesz.foundation.database.migration.StorageMigrator
import top.likoslupus.cellulosesz.foundation.database.schema.*
import java.nio.file.Path

class StorageMigrationTest {

    @TempDir
    lateinit var directory: Path

    private object TestSchema : SqlSchemaContributor {

        const val TABLE = "cz_test_items"
        override val id: String get() = "test"

        override fun tables(): List<SqlTable> =
            listOf(
                SqlTable(
                    TABLE,
                    listOf(
                        SqlColumn("namespace", SqlColumnType.NAME),
                        SqlColumn("id", SqlColumnType.NAME),
                        SqlColumn("payload", SqlColumnType.TEXT),
                    ),
                    listOf("namespace", "id"),
                )
            )

        override fun indexes(): List<SqlIndex> =
            emptyList()

    }

    /** An allowlisted table added since the last successful endpoint: present on the target, absent on the source. */
    private object NewTableSchema : SqlSchemaContributor {

        const val TABLE = "cz_warp_metadata"
        override val id: String get() = "new"

        override fun tables(): List<SqlTable> =
            listOf(
                SqlTable(
                    TABLE,
                    listOf(
                        SqlColumn("namespace", SqlColumnType.NAME),
                        SqlColumn("id", SqlColumnType.NAME),
                    ),
                    listOf("namespace", "id"),
                )
            )

        override fun indexes(): List<SqlIndex> =
            emptyList()

    }

    /** A required table absent from the source must never be skipped. */
    private object MissingRequiredSchema : SqlSchemaContributor {

        const val TABLE = "cz_test_required"
        override val id: String get() = "missing"

        override fun tables(): List<SqlTable> =
            listOf(
                SqlTable(
                    TABLE,
                    listOf(
                        SqlColumn("namespace", SqlColumnType.NAME),
                        SqlColumn("id", SqlColumnType.NAME),
                    ),
                    listOf("namespace", "id"),
                )
            )

        override fun indexes(): List<SqlIndex> =
            emptyList()

    }

    private fun descriptor(type: DatabaseType, path: String): ResolvedDatabaseDescriptor =
        DatabaseDescriptorResolver.resolve(
            when (type) {
                DatabaseType.SQLITE -> DatabaseSettings(sqlite = SqliteSettings(path = path))
                DatabaseType.H2 -> DatabaseSettings(
                    type = DatabaseType.H2,
                    h2 = H2Settings(path = path)
                )

                else -> error("unsupported in test")
            },
            directory
        )

    @Test
    fun `identity distinguishes endpoint but not credentials or pool`() {
        val base = descriptor(DatabaseType.SQLITE, "a.db")
        val otherPath = descriptor(DatabaseType.SQLITE, "b.db")
        assertNotEquals(base.identity, otherPath.identity)

        val sameLocation = descriptor(DatabaseType.SQLITE, "a.db")
        assertEquals(base.identity, sameLocation.identity)

        val secret = DatabaseSecretSpec.literal("s3cret")
        assertEquals(
            "s3cret",
            secret.resolve()
        )
        assertEquals(
            "env:KEY",
            DatabaseSecretSpec.environment("KEY").reference
        )
    }

    @Test
    fun `migrates rows from sqlite to h2 and is idempotent via journal`() {
        val sourceDescriptor = descriptor(DatabaseType.SQLITE, "source.db")
        HikariDatabaseRuntime.create(sourceDescriptor).use { source ->
            SchemaInitializer.initializeBlocking(
                source,
                listOf(MigrationJournalSchema, TestSchema)
            )
            source.transaction { connection ->
                listOf(
                    "n1" to "p1",
                    "n2" to "p2"
                ).forEach { (id, payload) ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${TestSchema.TABLE} (
                            namespace,
                            id,
                            payload
                        )
                        VALUES (
                            ?, ?, ?
                        )
                        """.trimIndent()
                    ).use {
                        it.setString(1, "default")
                        it.setString(2, id)
                        it.setString(3, payload)
                        it.executeUpdate()
                    }
                }
            }
        }

        val targetDescriptor = descriptor(DatabaseType.H2, "target")
        HikariDatabaseRuntime.create(targetDescriptor).use {
            SchemaInitializer.initializeBlocking(
                it,
                listOf(MigrationJournalSchema, TestSchema)
            )

            val result = StorageMigrator.migrateBlocking(
                source = sourceDescriptor,
                target = it,
                contributors = listOf(TestSchema),
                sourceNamespace = "default",
                targetNamespace = "default",
            )
            assertEquals(
                StorageMigrationResult.Completed(1, 2),
                result
            )

            val count = it.read { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM ${TestSchema.TABLE}")
                            .use { rs ->
                                rs.next()
                                rs.getInt(1)
                            }
                }
            }
            assertEquals(2, count)

            // A matching journal record makes a second attempt a no-op.
            val again = StorageMigrator.migrateBlocking(
                source = sourceDescriptor,
                target = it,
                contributors = listOf(TestSchema),
                sourceNamespace = "default",
                targetNamespace = "default",
            )
            assertEquals(
                StorageMigrationResult.AlreadyCompleted,
                again
            )
        }
    }

    @Test
    fun `refuses to merge into a non-empty target`() {
        val sourceDescriptor = descriptor(DatabaseType.SQLITE, "source2.db")
        HikariDatabaseRuntime.create(sourceDescriptor).use { source ->
            SchemaInitializer.initializeBlocking(
                source,
                listOf(TestSchema)
            )
            source.transaction { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${TestSchema.TABLE} (
                        namespace,
                        id,
                        payload
                    )
                    VALUES (
                        'default',
                        'x',
                        'y'
                    );
                    """.trimIndent()
                ).use { it.executeUpdate() }
            }
        }

        val targetDescriptor = descriptor(DatabaseType.H2, "target2")
        HikariDatabaseRuntime.create(targetDescriptor).use { target ->
            SchemaInitializer.initializeBlocking(
                target,
                listOf(MigrationJournalSchema, TestSchema)
            )
            target.transaction { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${TestSchema.TABLE} (
                        namespace,
                        id,
                        payload
                    )
                    VALUES (
                        'default',
                        'existing',
                        'z'
                    );
                    """.trimIndent()
                ).use { it.executeUpdate() }
            }

            assertThrows(StorageException::class.java) {
                StorageMigrator.migrateBlocking(
                    source = sourceDescriptor,
                    target = target,
                    contributors = listOf(TestSchema),
                    sourceNamespace = "default",
                    targetNamespace = "default",
                )
            }
        }
    }

    @Test
    fun `journal dialect is available for every backend`() {
        DatabaseType.entries.forEach { type -> SqlDialects.of(type) }
    }

    @Test
    fun `an allowlisted table absent from the source is tolerated`() {
        val sourceDescriptor = descriptor(DatabaseType.SQLITE, "source3.db")
        HikariDatabaseRuntime.create(sourceDescriptor).use { source ->
            // Only TestSchema exists in the old source; NewTableSchema is a newly added table.
            SchemaInitializer.initializeBlocking(source, listOf(TestSchema))
            source.transaction { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${TestSchema.TABLE} (
                        namespace,
                        id,
                        payload
                    )
                    VALUES (
                        'default',
                        'a',
                        'b'
                    );
                    """.trimIndent()
                ).use { it.executeUpdate() }
            }
        }

        val targetDescriptor = descriptor(DatabaseType.H2, "target3")
        HikariDatabaseRuntime.create(targetDescriptor).use { target ->
            SchemaInitializer.initializeBlocking(
                target,
                listOf(MigrationJournalSchema, TestSchema, NewTableSchema)
            )

            val result = StorageMigrator.migrateBlocking(
                source = sourceDescriptor,
                target = target,
                contributors = listOf(TestSchema, NewTableSchema),
                sourceNamespace = "default",
                targetNamespace = "default",
            )

            assertEquals(
                StorageMigrationResult.Completed(2, 1),
                result
            )
        }
    }

    @Test
    fun `a non-allowlisted table absent from the source fails`() {
        val sourceDescriptor = descriptor(DatabaseType.SQLITE, "source4.db")
        HikariDatabaseRuntime.create(sourceDescriptor).use { source ->
            SchemaInitializer.initializeBlocking(source, listOf(TestSchema))
            source.transaction { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${TestSchema.TABLE} (
                        namespace,
                        id,
                        payload
                    )
                    VALUES (
                        'default',
                        'a',
                        'b'
                    );
                    """.trimIndent()
                ).use { it.executeUpdate() }
            }
        }

        val targetDescriptor = descriptor(DatabaseType.H2, "target4")
        HikariDatabaseRuntime.create(targetDescriptor).use { target ->
            SchemaInitializer.initializeBlocking(
                target,
                listOf(MigrationJournalSchema, TestSchema, MissingRequiredSchema)
            )

            assertThrows(StorageException::class.java) {
                StorageMigrator.migrateBlocking(
                    source = sourceDescriptor,
                    target = target,
                    contributors = listOf(TestSchema, MissingRequiredSchema),
                    sourceNamespace = "default",
                    targetNamespace = "default",
                )
            }
        }
    }

}
