package top.likoslupus.cellulosesz.movement.warp

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.bindPosition
import top.likoslupus.cellulosesz.movement.readStoredPosition
import java.sql.SQLException
import java.time.Instant

internal class JdbcWarpRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : WarpRepository {

    override suspend fun list(): List<Warp> =
        try {
            database.readIo { connection ->
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

    override suspend fun get(name: WarpName): Warp? =
        try {
            database.readIo { connection ->
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

    override suspend fun metadata(name: WarpName): WarpMetadata? =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        created_by,
                        created_at,
                        updated_at_ms
                    FROM
                        ${MovementSqlSchema.WARP_METADATA}
                    WHERE
                        namespace = ? AND warp_name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, name.value)
                    statement.executeQuery().use { rs ->
                        when {
                            rs.next() -> WarpMetadata(
                                createdBy = rs.getString(1),
                                createdAt = Instant.ofEpochMilli(rs.getLong(2)),
                                updatedAt = (rs.getObject(3) as? Number)
                                        ?.let { Instant.ofEpochMilli(it.toLong()) },
                            )

                            else -> null
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            throw WarpDataException("unable to read warp metadata '${name.value}'", exception)
        }

    override suspend fun put(warp: Warp, metadata: WarpMetadata) =
        try {
            database.transactionIo { connection ->
                val existing = connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        created_by,
                        created_at
                    FROM
                        ${MovementSqlSchema.WARP_METADATA}
                    WHERE
                        namespace = ? AND warp_name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, warp.name.value)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) rs.getString(1) to rs.getLong(2) else null
                    }
                }
                val createdBy = existing?.first ?: metadata.createdBy
                val createdAt = existing?.second ?: metadata.createdAt.toEpochMilli()
                val updatedAt = metadata.updatedAt?.toEpochMilli() ?: createdAt

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
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${MovementSqlSchema.WARP_METADATA}
                    WHERE
                        namespace = ? AND warp_name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, warp.name.value)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${MovementSqlSchema.WARP_METADATA} (
                        namespace,
                        warp_name,
                        created_by,
                        created_at,
                        updated_at_ms
                    )
                    VALUES (
                        ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, warp.name.value)
                    statement.setString(3, createdBy)
                    statement.setLong(4, createdAt)
                    statement.setLong(5, updatedAt)
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            throw WarpDataException("unable to write warp '${warp.name.value}'", exception)
        }

    override suspend fun remove(name: WarpName): Boolean =
        try {
            database.transactionIo { connection ->
                val removed = connection.prepareStatement(
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
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${MovementSqlSchema.WARP_METADATA}
                    WHERE
                        namespace = ? AND warp_name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, name.value)
                    statement.executeUpdate()
                }
                removed
            }
        } catch (exception: SQLException) {
            throw WarpDataException("unable to remove warp '${name.value}'", exception)
        }

}
