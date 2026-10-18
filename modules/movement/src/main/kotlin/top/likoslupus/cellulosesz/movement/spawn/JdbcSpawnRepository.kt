package top.likoslupus.cellulosesz.movement.spawn

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.bindPosition
import top.likoslupus.cellulosesz.movement.readStoredPosition
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.sql.SQLException

internal class JdbcSpawnRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : SpawnRepository {

    override suspend fun read(): StoredPosition? =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    "SELECT dimension, x, y, z, yaw, pitch FROM ${MovementSqlSchema.SPAWN} WHERE namespace = ?"
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) rs.readStoredPosition(1) else null
                    }
                }
            }
        } catch (exception: SQLException) {
            throw SpawnDataException("unable to read spawn", exception)
        }

    override suspend fun write(position: StoredPosition?) =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    "DELETE FROM ${MovementSqlSchema.SPAWN} WHERE namespace = ?"
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeUpdate()
                }
                if (position != null) {
                    connection.prepareStatement(
                        "INSERT INTO ${MovementSqlSchema.SPAWN} " +
                                "(namespace, dimension, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.bindPosition(2, position)
                        statement.executeUpdate()
                    }
                }
            }
        } catch (exception: SQLException) {
            throw SpawnDataException("unable to write spawn", exception)
        }

}
