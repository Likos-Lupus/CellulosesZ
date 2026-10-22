package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.driver.DriverProvision
import top.likoslupus.cellulosesz.foundation.database.driver.DriverSource
import top.likoslupus.cellulosesz.foundation.database.driver.JdbcDriverProvisioner
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
    public val driverSource: DriverSource,
)

/**
 * Blocking cold-start storage bring-up. Order: provision the driver (fail fast) -> connect ->
 * create/probe schema -> detect endpoint change -> migrate if needed (idempotent,
 * journal-protected) -> record the successful endpoint. The active connection is never swapped at
 * runtime.
 *
 * Driver provisioning runs here, before the first tick, on the same thread that already opens the
 * JDBC connection; it never runs while the server is ticking (see ADR 0024).
 */
public class StorageBootstrap(
    private val storageRoot: Path,
    private val driverRoot: Path,
    private val businessContributors: List<SqlSchemaContributor>,
) {

    private val schemaContributors: List<SqlSchemaContributor> =
        listOf(MigrationJournalSchema) + businessContributors

    private val stateStore: LastStorageStateStore =
        LastStorageStateStore(storageRoot.resolve(".storage"))

    public fun startBlocking(settings: DatabaseSettings): StorageBootstrapResult {
        val drivers = JdbcDriverProvisioner.create(settings.driver, driverRoot)
        val target = DatabaseDescriptorResolver.resolve(settings, storageRoot)
        val targetDriver = drivers.provision(target).readyOrThrow("active")
        Files.createDirectories(storageRoot)
        target.ensureLocalStorageDirectories()
        val runtime = HikariDatabaseRuntime.create(
            target,
            targetDriver.handle.externalDataSource
        )

        try {
            val last = stateStore.read()
            val targetHash = target.identity.hash()
            var migration = "NONE"

            SchemaInitializer.initializeBlocking(runtime, schemaContributors)

            if (last != null
                && last.identity.hash() != targetHash
            ) {
                migration = migrate(last, target, runtime, drivers)
            }

            stateStore.write(
                LastStorageState.from(
                    target,
                    System.currentTimeMillis()
                )
            )
            return StorageBootstrapResult(
                database = runtime,
                descriptor = target,
                migration = migration,
                driverSource = targetDriver.source,
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
        drivers: JdbcDriverProvisioner,
    ): String {
        val journal = MigrationJournal(SqlDialects.of(runtime.type))
        val sourceHash = last.identity.hash()
        val targetHash = target.identity.hash()

        // A completed marker means a prior migration committed; finalize without recopying.
        journal.completed(
            runtime,
            sourceHash,
            targetHash,
        )?.let { return "COMPLETED" }

        // The source endpoint may use a different backend than the target, so its driver has to be
        // provisioned as well; otherwise a switch from e.g. MySQL to SQLite could never migrate.
        val source = last.toDescriptor()
        val sourceDriver = drivers.provision(source).readyOrThrow("previous")

        val result = StorageMigrator.migrateBlocking(
            source = source,
            sourceDataSource = sourceDriver.handle.externalDataSource,
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

    private fun DriverProvision.readyOrThrow(role: String): DriverProvision.Ready =
        when (this) {
            is DriverProvision.Ready -> this
            is DriverProvision.Unavailable -> throw StorageException(
                "cannot start the $role database: $message"
            )
        }

}
