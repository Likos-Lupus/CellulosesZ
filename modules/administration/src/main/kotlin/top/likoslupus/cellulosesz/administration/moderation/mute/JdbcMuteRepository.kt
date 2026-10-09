package top.likoslupus.cellulosesz.administration.moderation.mute

import com.mojang.logging.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.administration.AdministrationSqlSchema
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.util.*

/**
 * Active mutes in `cz_mutes`. A row that fails to map is isolated and reported (never overwritten),
 * so a single bad record cannot take down enforcement for every player.
 */
internal class JdbcMuteRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : MuteRepository {

    override suspend fun loadAll(): MuteLoadResult =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            player_uuid,
                            player_name,
                            actor_type,
                            actor_uuid,
                            actor_name,
                            reason,
                            issued_at_ms,
                            expires_at_ms
                        FROM
                            ${AdministrationSqlSchema.MUTES}
                        WHERE
                            namespace = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.executeQuery().use { rs ->
                            val records = mutableListOf<Mute>()
                            val corrupt = mutableSetOf<UUID>()
                            while (rs.next()) {
                                val rawId = rs.getString(1)
                                val playerId = try {
                                    UUID.fromString(rawId)
                                } catch (_: IllegalArgumentException) {
                                    LOGGER.error(
                                        "ignoring mute row with non-UUID player_uuid '{}'",
                                        rawId
                                    )
                                    continue
                                }
                                try {
                                    records += map(
                                        playerId,
                                        rs.getString(2),
                                        rs.getString(3),
                                        rs.getString(4),
                                        rs.getString(5),
                                        rs.getString(6),
                                        rs.getLong(7),
                                        rs.getObject(8)
                                                ?.let { rs.getLong(8) }
                                    )
                                } catch (exception: Exception) {
                                    LOGGER.error(
                                        "corrupt mute row for {} is kept and marked unavailable",
                                        playerId,
                                        exception
                                    )
                                    corrupt += playerId
                                }
                            }
                            MuteLoadResult(records, corrupt)
                        }
                    }
                }
            } catch (exception: SQLException) {
                throw MuteDataException("unable to load mutes", exception)
            }
        }

    override suspend fun put(mute: Mute) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${AdministrationSqlSchema.MUTES}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, mute.playerId.toString())
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${AdministrationSqlSchema.MUTES} (
                            namespace,
                            player_uuid,
                            player_name,
                            actor_type,
                            actor_uuid,
                            actor_name,
                            reason,
                            issued_at_ms,
                            expires_at_ms
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, mute.playerId.toString())
                        statement.setString(3, mute.playerName)
                        statement.setString(4, mute.actor.type)
                        when (mute.actor.id) {
                            null -> statement.setNull(5, Types.VARCHAR)
                            else -> statement.setString(5, mute.actor.id)
                        }
                        statement.setString(6, mute.actor.name)
                        statement.setString(7, mute.reason)
                        statement.setLong(8, mute.issuedAt.toEpochMilli())
                        mute.expiresAt?.let { statement.setLong(9, it.toEpochMilli()) }
                            ?: statement.setNull(9, Types.BIGINT)
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw MuteDataException("unable to write mute for ${mute.playerId}", exception)
            }
        }

    override suspend fun remove(playerId: UUID): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${AdministrationSqlSchema.MUTES}
                        WHERE
                            namespace = ? AND player_uuid = ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw MuteDataException("unable to remove mute for $playerId", exception)
            }
        }

    private fun map(
        playerId: UUID,
        playerName: String,
        actorType: String,
        actorId: String?,
        actorName: String,
        reason: String,
        issuedAtMs: Long,
        expiresAtMs: Long?,
    ): Mute {
        if (playerName.isBlank()) {
            throw MuteDataException("mute has a blank player name")
        }
        if (expiresAtMs != null && expiresAtMs <= issuedAtMs) {
            throw MuteDataException("mute expiry is not after its issue time")
        }
        return Mute(
            playerId = playerId,
            playerName = playerName,
            actor = PersistedModerationActor(
                type = actorType,
                id = actorId,
                name = actorName
            ),
            reason = reason,
            issuedAt = Instant.ofEpochMilli(issuedAtMs),
            expiresAt = expiresAtMs?.let(Instant::ofEpochMilli),
        )
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
