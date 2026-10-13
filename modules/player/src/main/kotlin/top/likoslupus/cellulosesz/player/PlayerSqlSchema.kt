package top.likoslupus.cellulosesz.player

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Player-context tables (sessions, nicknames). Added incrementally as features land. */
public object PlayerSqlSchema : SqlSchemaContributor {

    public const val SESSIONS: String = "cz_player_sessions"
    public const val NICKNAMES: String = "cz_player_nicknames"

    public override val id: String get() = "player"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = SESSIONS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("session_uuid", SqlColumnType.UUID),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("login_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("last_heartbeat_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("closed_at_ms", SqlColumnType.BIGINT, nullable = true),
                    SqlColumn("close_kind", SqlColumnType.NAME, nullable = true),
                ),
                primaryKey = listOf("namespace", "session_uuid"),
            ),
            SqlTable(
                name = NICKNAMES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("nickname_display", SqlColumnType.RESOURCE),
                    SqlColumn("normalized_nickname", SqlColumnType.RESOURCE),
                    SqlColumn("updated_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("updated_by_uuid", SqlColumnType.NAME, nullable = true),
                ),
                primaryKey = listOf("namespace", "player_uuid"),
            ),
        )

    public override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                "cz_player_sessions_player_closed",
                SESSIONS,
                listOf("namespace", "player_uuid", "closed_at_ms")
            ),
            SqlIndex(
                "cz_player_sessions_player_login",
                SESSIONS,
                listOf("namespace", "player_uuid", "login_at_ms")
            ),
            SqlIndex(
                "cz_player_nicknames_normalized",
                NICKNAMES,
                listOf("namespace", "normalized_nickname"),
                unique = true,
            ),
        )

}
