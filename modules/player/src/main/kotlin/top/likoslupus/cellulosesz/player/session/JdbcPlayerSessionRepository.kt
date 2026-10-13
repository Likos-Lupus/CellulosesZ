package top.likoslupus.cellulosesz.player.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.player.PlayerDataException
import top.likoslupus.cellulosesz.player.PlayerSqlSchema
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.util.*

/** JDBC sessions in `cz_player_sessions`. A row that fails to map is reported, never overwritten. */
internal class JdbcPlayerSessionRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : PlayerSessionRepository {

    override suspend fun begin(session: PlayerSession): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    if (exists(connection, session.sessionId)) {
                        return@transaction false
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${PlayerSqlSchema.SESSIONS} (
                            namespace,
                            session_uuid,
                            player_uuid,
                            login_at_ms,
                            last_heartbeat_at_ms,
                            closed_at_ms,
                            close_kind
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, session.sessionId.toString())
                        statement.setString(3, session.playerId.toString())
                        statement.setLong(4, session.loginAt.toEpochMilli())
                        statement.setLong(5, session.lastHeartbeatAt.toEpochMilli())
                        statement.setNull(6, Types.BIGINT)
                        statement.setNull(7, Types.VARCHAR)
                        statement.executeUpdate()
                    }
                    true
                }
            } catch (exception: SQLException) {
                throw PlayerDataException(
                    "unable to begin session for ${session.playerId}",
                    exception
                )
            }
        }

    override suspend fun heartbeat(sessionId: UUID, at: Instant): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        UPDATE ${PlayerSqlSchema.SESSIONS}
                        SET
                            last_heartbeat_at_ms = ?
                        WHERE
                            namespace = ? AND session_uuid = ? AND closed_at_ms IS NULL;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setLong(1, at.toEpochMilli())
                        statement.setString(2, namespace)
                        statement.setString(3, sessionId.toString())
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw PlayerDataException("unable to heartbeat session $sessionId", exception)
            }
        }

    override suspend fun close(
        sessionId: UUID,
        at: Instant,
        kind: SessionCloseKind,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        UPDATE ${PlayerSqlSchema.SESSIONS}
                        SET
                            closed_at_ms = ?,
                            close_kind = ?
                        WHERE
                            namespace = ? AND session_uuid = ? AND closed_at_ms IS NULL;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setLong(1, at.toEpochMilli())
                        statement.setString(2, kind.name)
                        statement.setString(3, namespace)
                        statement.setString(4, sessionId.toString())
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw PlayerDataException("unable to close session $sessionId", exception)
            }
        }

    override fun closeAllOpenBlocking(at: Instant, kind: SessionCloseKind): Int =
        try {
            database.transaction { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    UPDATE ${PlayerSqlSchema.SESSIONS}
                    SET
                        closed_at_ms = ?,
                        close_kind = ?
                    WHERE
                        namespace = ? AND closed_at_ms IS NULL;
                    """.trimIndent()
                ).use { statement ->
                    statement.setLong(1, at.toEpochMilli())
                    statement.setString(2, kind.name)
                    statement.setString(3, namespace)
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw PlayerDataException("unable to close open sessions", exception)
        }

    override fun recoverOpenBlocking(): Int =
        try {
            database.transaction { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    UPDATE ${PlayerSqlSchema.SESSIONS}
                    SET
                        closed_at_ms = last_heartbeat_at_ms,
                        close_kind = ?
                    WHERE
                        namespace = ? AND closed_at_ms IS NULL;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, SessionCloseKind.RECOVERED.name)
                    statement.setString(2, namespace)
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw PlayerDataException("unable to recover open sessions", exception)
        }

    override suspend fun loadSessions(playerId: UUID): List<PlayerSession> =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            session_uuid,
                            player_uuid,
                            login_at_ms,
                            last_heartbeat_at_ms,
                            closed_at_ms,
                            close_kind
                        FROM
                            ${PlayerSqlSchema.SESSIONS}
                        WHERE
                            namespace = ? AND player_uuid = ?
                        ORDER BY
                            login_at_ms DESC;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(map(rs))
                                }
                            }
                        }
                    }
                }
            } catch (exception: SQLException) {
                throw PlayerDataException("unable to load sessions for $playerId", exception)
            }
        }

    private fun exists(connection: Connection, sessionId: UUID): Boolean =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                session_uuid
            FROM
                ${PlayerSqlSchema.SESSIONS}
            WHERE
                namespace = ? AND session_uuid = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, sessionId.toString())
            statement.executeQuery().use { rs -> rs.next() }
        }

    private fun map(rs: ResultSet): PlayerSession {
        val sessionId = parseUuid(rs.getString(1), "session_uuid")
        val playerId = parseUuid(rs.getString(2), "player_uuid")
        val loginAt = Instant.ofEpochMilli(rs.getLong(3))
        val heartbeatAt = Instant.ofEpochMilli(rs.getLong(4))
        val closedAt = rs.getObject(5)?.let {
            Instant.ofEpochMilli(rs.getLong(5))
        }
        val rawKind = rs.getString(6)
        val closeKind = when {
            rawKind == null -> null
            else -> SessionCloseKind.entries.firstOrNull { it.name == rawKind }
                ?: throw PlayerDataException("session $sessionId has an unknown close kind '$rawKind'")
        }
        if (closedAt == null && closeKind != null) {
            throw PlayerDataException("session $sessionId is open but has a close kind")
        }
        if (closedAt != null && closeKind == null) {
            throw PlayerDataException("session $sessionId is closed but has no close kind")
        }
        if (heartbeatAt.isBefore(loginAt)) {
            throw PlayerDataException("session $sessionId heartbeats before it started")
        }
        return PlayerSession(
            sessionId = sessionId,
            playerId = playerId,
            loginAt = loginAt,
            lastHeartbeatAt = heartbeatAt,
            closedAt = closedAt,
            closeKind = closeKind,
        )
    }

    private fun parseUuid(raw: String, label: String): UUID =
        try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            throw PlayerDataException("session row has an invalid $label")
        }

}
