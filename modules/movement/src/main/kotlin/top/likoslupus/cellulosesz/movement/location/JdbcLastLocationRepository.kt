package top.likoslupus.cellulosesz.movement.location

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.movement.bindPosition
import top.likoslupus.cellulosesz.movement.readStoredPosition
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.sql.SQLException
import java.time.Instant
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class JdbcLastLocationRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : LastLocationRepository {

    override suspend fun write(
        playerId: UUID,
        position: StoredPosition,
        updatedAt: Instant,
    ) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${MovementSqlSchema.LAST_LOCATIONS}
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
                            ${MovementSqlSchema.LAST_LOCATIONS} (
                                namespace,
                                player_uuid,
                                dimension,
                                x,
                                y,
                                z,
                                yaw,
                                pitch,
                                updated_at_ms
                            )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.bindPosition(3, position)
                        statement.setLong(9, updatedAt.toEpochMilli())
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw LastLocationDataException(
                    "unable to write last location for $playerId",
                    exception,
                )
            }
        }

    override suspend fun read(playerId: UUID): LastLocation? =
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
                            pitch,
                            updated_at_ms
                        FROM
                            ${MovementSqlSchema.LAST_LOCATIONS}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeQuery().use { rs ->
                            if (rs.next()) {
                                LastLocation(
                                    position = rs.readStoredPosition(1),
                                    updatedAt = Instant.ofEpochMilli(rs.getLong(7)),
                                )
                            } else {
                                null
                            }
                        }
                    }
                }
            } catch (exception: SQLException) {
                throw LastLocationDataException(
                    "unable to read last location for $playerId",
                    exception,
                )
            }
        }

}
