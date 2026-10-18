package top.likoslupus.cellulosesz.movement.random

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import java.sql.ResultSet
import java.sql.SQLException

internal class JdbcRandomTeleportRegionRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : RandomTeleportRegionRepository {

    override suspend fun list(): List<RandomTeleportRegion> =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        name,
                        dimension,
                        center_x,
                        center_z,
                        minimum_radius,
                        maximum_radius,
                        enabled,
                        revision
                    FROM
                        ${MovementSqlSchema.RANDOM_TELEPORT_REGIONS}
                    WHERE
                        namespace = ?
                    ORDER BY
                        name;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(readRegion(rs))
                            }
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            throw RandomTeleportDataException(
                "unable to read random teleport regions",
                exception
            )
        }

    override suspend fun find(name: RegionName): RandomTeleportRegion? =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        name,
                        dimension,
                        center_x,
                        center_z,
                        minimum_radius,
                        maximum_radius,
                        enabled,
                        revision
                    FROM
                        ${MovementSqlSchema.RANDOM_TELEPORT_REGIONS}
                    WHERE
                        namespace = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, name.value)
                    statement.executeQuery().use { rs ->
                        when {
                            rs.next() -> readRegion(rs)
                            else -> null
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            throw RandomTeleportDataException(
                "unable to read random teleport region '${name.value}'",
                exception,
            )
        }

    override suspend fun count(): Int =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        COUNT(*)
                    FROM
                        ${MovementSqlSchema.RANDOM_TELEPORT_REGIONS}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) rs.getInt(1) else 0
                    }
                }
            }
        } catch (exception: SQLException) {
            throw RandomTeleportDataException(
                "unable to count random teleport regions",
                exception
            )
        }

    override suspend fun put(region: RandomTeleportRegion) =
        try {
            database.transactionIo { connection ->
                val revision = connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        revision
                    FROM
                        ${MovementSqlSchema.RANDOM_TELEPORT_REGIONS}
                    WHERE
                        namespace = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, region.name.value)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) rs.getLong(1) else 0L
                    }
                } + 1L

                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${MovementSqlSchema.RANDOM_TELEPORT_REGIONS}
                    WHERE
                        namespace = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, region.name.value)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${MovementSqlSchema.RANDOM_TELEPORT_REGIONS} (
                        namespace,
                        name,
                        dimension,
                        center_x,
                        center_z,
                        minimum_radius,
                        maximum_radius,
                        enabled,
                        revision
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, region.name.value)
                    statement.setString(3, region.dimension)
                    statement.setDouble(4, region.centerX)
                    statement.setDouble(5, region.centerZ)
                    statement.setDouble(6, region.minimumRadius)
                    statement.setDouble(7, region.maximumRadius)
                    statement.setBoolean(8, region.enabled)
                    statement.setLong(9, revision)
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            throw RandomTeleportDataException(
                "unable to write random teleport region '${region.name.value}'",
                exception,
            )
        }

    override suspend fun remove(name: RegionName): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${MovementSqlSchema.RANDOM_TELEPORT_REGIONS}
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
            throw RandomTeleportDataException(
                "unable to remove random teleport region '${name.value}'",
                exception,
            )
        }

    private fun readRegion(rs: ResultSet): RandomTeleportRegion {
        val name = RegionName.parse(rs.getString(1))
            ?: throw RandomTeleportDataException("corrupt random teleport region name")
        val minimumRadius = rs.getDouble(5)
        val maximumRadius = rs.getDouble(6)
        if (!minimumRadius.isFinite() || !maximumRadius.isFinite()
            || minimumRadius < 0.0 || minimumRadius >= maximumRadius
        ) {
            throw RandomTeleportDataException(
                "corrupt radius range for random teleport region '${name.value}'"
            )
        }
        return RandomTeleportRegion(
            name = name,
            dimension = rs.getString(2),
            centerX = rs.getDouble(3),
            centerZ = rs.getDouble(4),
            minimumRadius = minimumRadius,
            maximumRadius = maximumRadius,
            enabled = rs.getBoolean(7),
            revision = rs.getLong(8),
        )
    }

}
