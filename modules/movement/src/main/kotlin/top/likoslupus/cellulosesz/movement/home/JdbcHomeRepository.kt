package top.likoslupus.cellulosesz.movement.home

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.bindPosition
import top.likoslupus.cellulosesz.movement.readStoredPosition
import java.sql.SQLException
import java.util.*

internal class JdbcHomeRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : HomeRepository {

    override suspend fun list(owner: UUID): List<Home> =
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
                        ${MovementSqlSchema.HOMES}
                    WHERE
                        namespace = ? AND owner_uuid = ?
                    ORDER BY
                        name;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                val name = HomeName.parse(rs.getString(1))
                                    ?: throw HomeDataException("corrupt home name for $owner")
                                add(Home(name, rs.readStoredPosition(2)))
                            }
                        }
                    }
                }
            }
        } catch (exception: SQLException) {
            throw HomeDataException("unable to read homes for $owner", exception)
        }

    override suspend fun put(owner: UUID, home: Home) =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${MovementSqlSchema.HOMES}
                    WHERE
                        namespace = ? AND owner_uuid = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.setString(3, home.name.value)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${MovementSqlSchema.HOMES} (
                        namespace,
                        owner_uuid,
                        name,
                        dimension,
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.setString(3, home.name.value)
                    statement.bindPosition(4, home.position)
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            throw HomeDataException(
                "unable to write home '${home.name.value}' for $owner",
                exception
            )
        }

    override suspend fun remove(owner: UUID, name: HomeName): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${MovementSqlSchema.HOMES}
                    WHERE
                        namespace = ? AND owner_uuid = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.setString(3, name.value)
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw HomeDataException(
                "unable to remove home '${name.value}' for $owner",
                exception
            )
        }

    override suspend fun rename(
        owner: UUID,
        from: HomeName,
        to: HomeName,
    ): HomeRenameOutcome =
        try {
            database.transactionIo { connection ->
                val position = connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        dimension,
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                    FROM
                        ${MovementSqlSchema.HOMES}
                    WHERE
                        namespace = ? AND owner_uuid = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.setString(3, from.value)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) rs.readStoredPosition(1) else null
                    }
                } ?: return@transactionIo HomeRenameOutcome.SourceMissing

                val destinationExists = connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        1
                    FROM
                        ${MovementSqlSchema.HOMES}
                    WHERE
                        namespace = ? AND owner_uuid = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.setString(3, to.value)
                    statement.executeQuery().use { rs -> rs.next() }
                }
                if (destinationExists) {
                    return@transactionIo HomeRenameOutcome.DestinationExists
                }

                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${MovementSqlSchema.HOMES}
                    WHERE
                        namespace = ? AND owner_uuid = ? AND name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.setString(3, from.value)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO
                        ${MovementSqlSchema.HOMES} (
                            namespace,
                            owner_uuid,
                            name,
                            dimension,
                            x,
                            y,
                            z,
                            yaw,
                            pitch
                        )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, owner.toString())
                    statement.setString(3, to.value)
                    statement.bindPosition(4, position)
                    statement.executeUpdate()
                }
                HomeRenameOutcome.Renamed
            }
        } catch (exception: SQLException) {
            throw HomeDataException(
                "unable to rename home '${from.value}' to '${to.value}' for $owner",
                exception
            )
        }

}
