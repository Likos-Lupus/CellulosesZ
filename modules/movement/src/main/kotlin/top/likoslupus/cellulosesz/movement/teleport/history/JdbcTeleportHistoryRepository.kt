package top.likoslupus.cellulosesz.movement.teleport.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.bindPosition
import top.likoslupus.cellulosesz.movement.readStoredPosition
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.sql.SQLException
import java.util.*

internal class JdbcTeleportHistoryRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : TeleportHistoryRepository {

    override suspend fun read(playerId: UUID): StoredPosition? =
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
                            ${MovementSqlSchema.TELEPORT_HISTORY}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeQuery().use { rs ->
                            if (rs.next()) rs.readStoredPosition(1) else null
                        }
                    }
                }
            } catch (exception: SQLException) {
                throw TeleportHistoryDataException(
                    "unable to read teleport history for $playerId",
                    exception
                )
            }
        }

    override suspend fun write(playerId: UUID, position: StoredPosition) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${MovementSqlSchema.TELEPORT_HISTORY}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO
                            ${MovementSqlSchema.TELEPORT_HISTORY} (
                                namespace,
                                player_uuid,
                                dimension,
                                x,
                                y,
                                z,
                                yaw,
                                pitch
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?
                        )
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.bindPosition(3, position)
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw TeleportHistoryDataException(
                    "unable to write teleport history for $playerId",
                    exception
                )
            }
        }

}
