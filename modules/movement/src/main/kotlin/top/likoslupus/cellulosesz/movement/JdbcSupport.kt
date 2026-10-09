package top.likoslupus.cellulosesz.movement

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.sql.PreparedStatement
import java.sql.ResultSet

/** Shared JDBC column binding/reading for the movement position layout. */
internal fun PreparedStatement.bindPosition(at: Int, position: StoredPosition) {
    setString(at, position.dimension)
    setDouble(at + 1, position.x)
    setDouble(at + 2, position.y)
    setDouble(at + 3, position.z)
    setFloat(at + 4, position.yaw)
    setFloat(at + 5, position.pitch)
}

internal fun ResultSet.readStoredPosition(at: Int): StoredPosition =
    StoredPosition(
        dimension = getString(at),
        x = getDouble(at + 1),
        y = getDouble(at + 2),
        z = getDouble(at + 3),
        yaw = getFloat(at + 4),
        pitch = getFloat(at + 5),
    )
