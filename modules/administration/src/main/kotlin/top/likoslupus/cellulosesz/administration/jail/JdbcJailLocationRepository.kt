package top.likoslupus.cellulosesz.administration.jail

import top.likoslupus.cellulosesz.administration.AdministrationSqlSchema
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import java.sql.SQLException
import java.sql.Types
import java.time.Instant

/** Jail locations in `cz_jail_locations`. */
internal class JdbcJailLocationRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : JailLocationRepository {

    override suspend fun loadAll(): List<JailLocation> =
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
                        pitch,
                        radius,
                        created_at_ms,
                        created_by
                    FROM
                        ${AdministrationSqlSchema.JAIL_LOCATIONS}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        val locations = mutableListOf<JailLocation>()
                        while (rs.next()) {
                            val rawName = rs.getString(1)
                            val name = JailName.parse(rawName)
                                ?: throw JailDataException(
                                    "jail location has an invalid name '$rawName'"
                                )
                            locations += JailLocation(
                                name = name,
                                dimension = rs.getString(2),
                                x = rs.getDouble(3),
                                y = rs.getDouble(4),
                                z = rs.getDouble(5),
                                yaw = rs.getFloat(6),
                                pitch = rs.getFloat(7),
                                radius = rs.getDouble(8),
                                createdAt = Instant.ofEpochMilli(rs.getLong(9)),
                                createdBy = rs.getString(10),
                            )
                        }
                        locations
                    }
                }
            }
        } catch (exception: SQLException) {
            throw JailDataException("unable to load jail locations", exception)
        }

    override suspend fun put(location: JailLocation) =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${AdministrationSqlSchema.JAIL_LOCATIONS}
                    WHERE
                        namespace = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, location.name.value)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${AdministrationSqlSchema.JAIL_LOCATIONS} (
                        namespace,
                        name,
                        dimension,
                        x,
                        y,
                        z,
                        yaw,
                        pitch,
                        radius,
                        created_at_ms,
                        created_by
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, location.name.value)
                    statement.setString(3, location.dimension)
                    statement.setDouble(4, location.x)
                    statement.setDouble(5, location.y)
                    statement.setDouble(6, location.z)
                    statement.setFloat(7, location.yaw)
                    statement.setFloat(8, location.pitch)
                    statement.setDouble(9, location.radius)
                    statement.setLong(10, location.createdAt.toEpochMilli())
                    location.createdBy?.let {
                        statement.setString(11, it)
                    } ?: statement.setNull(11, Types.VARCHAR)
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            throw JailDataException(
                "unable to write jail location ${location.name.value}",
                exception
            )
        }

    override suspend fun remove(name: JailName): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${AdministrationSqlSchema.JAIL_LOCATIONS}
                    WHERE
                        namespace = ? AND name = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, name.value)
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw JailDataException("unable to remove jail location ${name.value}", exception)
        }

}
