package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.migration.MigrationJournal
import top.likoslupus.cellulosesz.foundation.database.migration.MigrationJournalSchema
import top.likoslupus.cellulosesz.foundation.database.migration.StorageMigrationResult
import top.likoslupus.cellulosesz.foundation.database.migration.StorageMigrator
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import top.likoslupus.cellulosesz.foundation.database.schema.SqlDialects
import top.likoslupus.cellulosesz.foundation.database.schema.SqlSchemaContributor
import top.likoslupus.cellulosesz.foundation.database.state.LastStorageState
import top.likoslupus.cellulosesz.foundation.database.state.LastStorageStateStore
import java.nio.file.Files
import java.nio.file.Path

/** The bound runtime plus a human-readable migration summary. */
public data class StorageBootstrapResult(
    public val database: DatabaseRuntime,
    public val descriptor: ResolvedDatabaseDescriptor,
    public val migration: String,
)

/**
 * Blocking cold-start storage bring-up. Order: connect (fail fast) -> create/probe schema -> detect
 * endpoint change -> migrate if needed (idempotent, journal-protected) -> record the successful
 * endpoint. The active connection is never swapped at runtime.
 */
public class StorageBootstrap(
    private val storageRoot: Path,
    private val businessContributors: List<SqlSchemaContributor>,
) {

    private val schemaContributors: List<SqlSchemaContributor> =
        listOf(MigrationJournalSchema) + businessContributors

    private val stateStore: LastStorageStateStore =
        LastStorageStateStore(storageRoot.resolve(".storage"))

    public fun startBlocking(settings: DatabaseSettings): StorageBootstrapResult {
        val target = DatabaseDescriptorResolver.resolve(settings, storageRoot)
        Files.createDirectories(storageRoot)
        target.ensureLocalStorageDirectories()
        val runtime = HikariDatabaseRuntime.create(target)

        try {
            val last = stateStore.read()
            val targetHash = target.identity.hash()
            var migration = "NONE"

            SchemaInitializer.initializeBlocking(runtime, schemaContributors)

            if (last != null
                && last.identity.hash() != targetHash
            ) {
                migration = migrate(last, target, runtime)
            }

            stateStore.write(
                LastStorageState.from(
                    target,
                    System.currentTimeMillis()
                )
            )
            return StorageBootstrapResult(
                runtime,
                target,
                migration
            )
        } catch (exception: Throwable) {
            runtime.close()
            throw exception
        }
    }

    private fun migrate(
        last: LastStorageState,
        target: ResolvedDatabaseDescriptor,
        runtime: DatabaseRuntime,
    ): String {
        val journal = MigrationJournal(SqlDialects.of(runtime.type))
        val sourceHash = last.identity.hash()
        val targetHash = target.identity.hash()

        // A completed marker means a prior migration committed; finalize without recopying.
        journal.completed(
            runtime,
            sourceHash,
            targetHash
        )?.let { return "COMPLETED" }

        val result = StorageMigrator.migrateBlocking(
            source = last.toDescriptor(),
            target = runtime,
            contributors = businessContributors,
            sourceNamespace = last.namespace,
            targetNamespace = target.namespace,
        )
        return when (result) {
            StorageMigrationResult.AlreadyCompleted -> "COMPLETED"
            is StorageMigrationResult.Completed -> "COPIED (${result.tables} tables, ${result.rows} rows)"
        }
    }

}
