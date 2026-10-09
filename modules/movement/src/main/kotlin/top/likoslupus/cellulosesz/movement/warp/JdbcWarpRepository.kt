package top.likoslupus.cellulosesz.movement.warp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.bindPosition
import top.likoslupus.cellulosesz.movement.readStoredPosition
import java.sql.SQLException

internal class JdbcWarpRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : WarpRepository {

    override suspend fun list(): List<Warp> =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                    SELECT
                        name,
                        dimension,
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                    FROM
                        ${MovementSqlSchema.WARPS}
                    WHERE
                        namespace = ?
                    ORDER BY
                        name
                    """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    val name = WarpName.parse(rs.getString(1))
                                        ?: throw WarpDataException("corrupt warp name")
                                    add(Warp(name, rs.readStoredPosition(2)))
                                }
                            }
                        }
                    }
                }
            } catch (exception: SQLException) {
                throw WarpDataException("unable to read warps", exception)
            }
        }

    override suspend fun get(name: WarpName): Warp? =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            dimension,
                            x,
                            y,
                            z,
                            yaw,
                            pitch
                        FROM
                            ${MovementSqlSchema.WARPS}
                        WHERE
                            namespace = ? AND name = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, name.value)
                        statement.executeQuery().use { rs ->
                            when {
                                rs.next() -> Warp(name, rs.readStoredPosition(1))
                                else -> null
                            }
                        }
                    }
                }
            } catch (exception: SQLException) {
                throw WarpDataException("unable to read warp '${name.value}'", exception)
            }
        }

    override suspend fun put(warp: Warp) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${MovementSqlSchema.WARPS}
                        WHERE
                            namespace = ? AND name = ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, warp.name.value)
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${MovementSqlSchema.WARPS} (
                            namespace,
                            name,
                            dimension,
                            x,
                            y,
                            z,
                            yaw,
                            pitch
                        ) 
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, warp.name.value)
                        statement.bindPosition(3, warp.position)
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw WarpDataException("unable to write warp '${warp.name.value}'", exception)
            }
        }

    override suspend fun remove(name: WarpName): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${MovementSqlSchema.WARPS}
                        WHERE
                            namespace = ? AND name = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, name.value)
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw WarpDataException("unable to remove warp '${name.value}'", exception)
            }
        }

}
