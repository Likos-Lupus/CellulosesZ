package top.likoslupus.cellulosesz.administration.jail

import top.likoslupus.cellulosesz.administration.AdministrationSqlSchema
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.core.player.WorldPosition
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.readIo
import top.likoslupus.cellulosesz.foundation.database.transactionIo
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.util.*

/** Jail sentences in `cz_jail_sentences`. */
internal class JdbcJailSentenceRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : JailSentenceRepository {

    override suspend fun loadAll(): List<JailSentence> =
        try {
            database.readIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        jail_name,
                        status,
                        issued_by,
                        reason,
                        issued_at_ms,
                        expires_at_ms,
                        return_dimension,
                        return_x,
                        return_y,
                        return_z,
                        return_yaw,
                        return_pitch,
                        release_requested_at_ms,
                        player_name,
                        sentence_mode,
                        remaining_seconds
                    FROM
                        ${AdministrationSqlSchema.JAIL_SENTENCES}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        val sentences = mutableListOf<JailSentence>()
                        while (rs.next()) {
                            val rawId = rs.getString(1)
                            val playerId = try {
                                UUID.fromString(rawId)
                            } catch (_: IllegalArgumentException) {
                                throw JailDataException(
                                    "jail sentence has a non-UUID player '$rawId'"
                                )
                            }
                            val rawJail = rs.getString(2)
                            val jailName = JailName.parse(rawJail)
                                ?: throw JailDataException(
                                    "jail sentence has an invalid jail name '$rawJail'"
                                )
                            val status = statusOf(rs.getString(3))
                            val issuedBy = rs.getString(4)
                            val issuedAtMs = rs.getLong(6)
                            val expiresAtMs = rs.getObject(7)?.let { rs.getLong(7) }
                            val returnDimension = rs.getString(8)
                            val returnPosition = returnDimension?.let { dimension ->
                                WorldPosition(
                                    dimension = dimension,
                                    x = rs.getDouble(9),
                                    y = rs.getDouble(10),
                                    z = rs.getDouble(11),
                                    yaw = rs.getFloat(12),
                                    pitch = rs.getFloat(13),
                                )
                            }
                            val releaseRequestedAtMs =
                                rs.getObject(14)?.let { rs.getLong(14) }
                            val mode = JailSentenceMode.parse(rs.getString(16))
                                ?: JailSentenceMode.PERMANENT
                            val remainingSeconds =
                                rs.getObject(17)?.let { rs.getLong(17) }
                            sentences += JailSentence(
                                playerId = playerId,
                                playerName = rs.getString(15),
                                jailName = jailName,
                                status = status,
                                actor = PersistedModerationActor(
                                    type = "player",
                                    id = null,
                                    name = issuedBy,
                                ),
                                reason = rs.getString(5),
                                issuedAt = Instant.ofEpochMilli(issuedAtMs),
                                mode = mode,
                                expiresAt = expiresAtMs?.let(Instant::ofEpochMilli),
                                remainingSeconds = remainingSeconds,
                                returnPosition = returnPosition,
                                releaseRequestedAt =
                                    releaseRequestedAtMs?.let(Instant::ofEpochMilli),
                            )
                        }
                        sentences
                    }
                }
            }
        } catch (exception: SQLException) {
            throw JailDataException("unable to load jail sentences", exception)
        }

    override suspend fun put(sentence: JailSentence) =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${AdministrationSqlSchema.JAIL_SENTENCES}
                    WHERE
                        namespace = ? AND player_uuid = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, sentence.playerId.toString())
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${AdministrationSqlSchema.JAIL_SENTENCES} (
                        namespace,
                        player_uuid,
                        jail_name,
                        status,
                        issued_by,
                        reason,
                        issued_at_ms,
                        expires_at_ms,
                        return_dimension,
                        return_x,
                        return_y,
                        return_z,
                        return_yaw,
                        return_pitch,
                        release_requested_at_ms,
                        player_name,
                        sentence_mode,
                        remaining_seconds
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, sentence.playerId.toString())
                    statement.setString(3, sentence.jailName.value)
                    statement.setString(4, sentence.status.name)
                    statement.setString(5, sentence.actor.name)
                    statement.setString(6, sentence.reason)
                    statement.setLong(7, sentence.issuedAt.toEpochMilli())
                    sentence.expiresAt?.let {
                        statement.setLong(8, it.toEpochMilli())
                    } ?: statement.setNull(8, Types.BIGINT)
                    val returnPosition = sentence.returnPosition
                    if (returnPosition == null) {
                        statement.setNull(9, Types.VARCHAR)
                        statement.setNull(10, Types.DOUBLE)
                        statement.setNull(11, Types.DOUBLE)
                        statement.setNull(12, Types.DOUBLE)
                        statement.setNull(13, Types.REAL)
                        statement.setNull(14, Types.REAL)
                    } else {
                        statement.setString(9, returnPosition.dimension)
                        statement.setDouble(10, returnPosition.x)
                        statement.setDouble(11, returnPosition.y)
                        statement.setDouble(12, returnPosition.z)
                        statement.setFloat(13, returnPosition.yaw)
                        statement.setFloat(14, returnPosition.pitch)
                    }
                    sentence.releaseRequestedAt?.let {
                        statement.setLong(15, it.toEpochMilli())
                    } ?: statement.setNull(15, Types.BIGINT)
                    statement.setString(16, sentence.playerName)
                    statement.setString(17, sentence.mode.name)
                    sentence.remainingSeconds?.let {
                        statement.setLong(18, it)
                    } ?: statement.setNull(18, Types.BIGINT)
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            throw JailDataException(
                "unable to write jail sentence for ${sentence.playerId}",
                exception
            )
        }

    override suspend fun updateStatus(
        playerId: UUID,
        status: JailStatus,
        releaseRequestedAt: Instant?,
    ): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    UPDATE
                        ${AdministrationSqlSchema.JAIL_SENTENCES}
                    SET
                        status = ?,
                        release_requested_at_ms = ?
                    WHERE
                        namespace = ? AND player_uuid = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, status.name)
                    releaseRequestedAt?.let {
                        statement.setLong(2, it.toEpochMilli())
                    } ?: statement.setNull(2, Types.BIGINT)
                    statement.setString(3, namespace)
                    statement.setString(4, playerId.toString())
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw JailDataException("unable to update jail sentence for $playerId", exception)
        }

    override suspend fun remove(playerId: UUID): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${AdministrationSqlSchema.JAIL_SENTENCES}
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
            throw JailDataException("unable to remove jail sentence for $playerId", exception)
        }

    override suspend fun updateRemaining(playerId: UUID, remainingSeconds: Long): Boolean =
        try {
            database.transactionIo { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    UPDATE
                        ${AdministrationSqlSchema.JAIL_SENTENCES}
                    SET
                        remaining_seconds = ?
                    WHERE
                        namespace = ? AND player_uuid = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setLong(1, remainingSeconds)
                    statement.setString(2, namespace)
                    statement.setString(3, playerId.toString())
                    statement.executeUpdate() > 0
                }
            }
        } catch (exception: SQLException) {
            throw JailDataException(
                "unable to update jail sentence remaining time for $playerId",
                exception,
            )
        }

    private fun statusOf(raw: String): JailStatus =
        try {
            JailStatus.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            throw JailDataException("jail sentence has an unknown status '$raw'")
        }

}
