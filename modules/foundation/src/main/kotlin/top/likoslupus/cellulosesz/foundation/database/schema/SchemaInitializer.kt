package top.likoslupus.cellulosesz.foundation.database.schema

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.sql.Connection
import java.sql.SQLException

/**
 * Creates the current schema and verifies it. This is *not* schema-version migration: it only
 * guarantees the tables/indexes this build needs exist and are readable. A same-named table with a
 * different shape fails the compatibility probe rather than being rewritten.
 */
public object SchemaInitializer {

    public fun initializeBlocking(
        runtime: DatabaseRuntime,
        contributors: List<SqlSchemaContributor>
    ) {
        val dialect = SqlDialects.of(runtime.type)
        try {
            runtime.transaction { connection ->
                contributors.forEach { contributor ->
                    contributor.tables().forEach { table ->
                        connection.createStatement().use {
                            it.executeUpdate(dialect.createTableSql(table))
                        }
                    }
                    contributor.indexes().forEach { index ->
                        executeIndex(connection, dialect, index)
                    }
                }
            }
            runtime.read { connection ->
                contributors.forEach { contributor ->
                    contributor.tables().forEach { table ->
                        connection.createStatement().use { statement ->
                            statement.executeQuery(dialect.probeSql(table)).close()
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            throw StorageException("storage schema initialization failed", exception)
        }
    }

    private fun executeIndex(
        connection: Connection,
        dialect: SqlDialect,
        index: SqlIndex
    ) {
        try {
            connection.createStatement().use {
                it.executeUpdate(dialect.createIndexSql(index))
            }
        } catch (exception: SQLException) {
            val duplicate = dialect.createIndexMayDuplicate &&
                    exception.message?.contains(
                        "Duplicate key name",
                        ignoreCase = true
                    ) == true
            if (!duplicate) {
                throw exception
            }
        }
    }

}
