package top.likoslupus.cellulosesz.player.nickname

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import top.likoslupus.cellulosesz.foundation.parseUuidOrThrow
import top.likoslupus.cellulosesz.player.PlayerDataException
import top.likoslupus.cellulosesz.player.PlayerSqlSchema
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import java.util.*

/** JDBC nicknames in `cz_player_nicknames`. A row that fails to map is reported, never overwritten. */
internal class JdbcNicknameRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : NicknameRepository {

    override suspend fun find(playerId: UUID): StoredNickname? =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        nickname_display,
                        normalized_nickname,
                        owner_name,
                        updated_at_ms
                    FROM
                        ${PlayerSqlSchema.NICKNAMES}
                    WHERE
                        namespace = ? AND player_uuid = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, playerId.toString())
                    statement.executeQuery().use { rs ->
                        if (rs.next()) map(rs) else null
                    }
                }
            }
        } catch (exception: SQLException) {
            throw PlayerDataException("unable to read nickname for $playerId", exception)
        }

    override suspend fun findByNormalized(normalized: String): StoredNickname? =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        nickname_display,
                        normalized_nickname,
                        owner_name,
                        updated_at_ms
                    FROM
                        ${PlayerSqlSchema.NICKNAMES}
                    WHERE
                        namespace = ? AND normalized_nickname = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, normalized)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) map(rs) else null
                    }
                }
            }
        } catch (exception: SQLException) {
            throw PlayerDataException("unable to read nickname '$normalized'", exception)
        }

    override suspend fun save(record: StoredNickname) =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${PlayerSqlSchema.NICKNAMES}
                    WHERE
                        namespace = ? AND player_uuid = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, record.playerId.toString())
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${PlayerSqlSchema.NICKNAMES} (
                        namespace,
                        player_uuid,
                        nickname_display,
                        normalized_nickname,
                        owner_name,
                        updated_at_ms,
                        updated_by_uuid
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, record.playerId.toString())
                    statement.setString(3, record.display)
                    statement.setString(4, record.normalized)
                    statement.setString(5, record.ownerName)
                    statement.setLong(6, record.updatedAt.toEpochMilli())
                    statement.setString(7, record.updatedBy?.toString())
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            throw PlayerDataException(
                "unable to write nickname for ${record.playerId}",
                exception
            )
        }

    override suspend fun delete(playerId: UUID): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${PlayerSqlSchema.NICKNAMES}
                    WHERE
                        namespace = ? AND player_uuid = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, playerId.toString())
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw PlayerDataException("unable to delete nickname for $playerId", exception)
        }

    private fun map(rs: ResultSet): StoredNickname {
        val playerId = parseUuid(rs.getString(1))
        val display = rs.getString(2)
        val normalized = rs.getString(3)
        if (display.isBlank() || normalized.isBlank()) {
            throw PlayerDataException("nickname row for $playerId is empty")
        }
        return StoredNickname(
            playerId = playerId,
            display = display,
            normalized = normalized,
            ownerName = rs.getString(4),
            updatedAt = Instant.ofEpochMilli(rs.getLong(5)),
        )
    }

    private fun parseUuid(raw: String): UUID =
        parseUuidOrThrow(raw) {
            throw PlayerDataException("nickname row has an invalid player_uuid")
        }

}
