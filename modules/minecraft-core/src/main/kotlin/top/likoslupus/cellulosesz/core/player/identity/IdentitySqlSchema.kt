package top.likoslupus.cellulosesz.core.player.identity

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Player identity tables. Names are normalized in code, never by a database collation. */
public object IdentitySqlSchema : SqlSchemaContributor {

    public const val PLAYERS: String = "cz_players"
    public const val PLAYER_NAMES: String = "cz_player_names"

    override val id: String
        get() = "identity"

    override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = PLAYERS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("current_name", SqlColumnType.NAME),
                    SqlColumn("normalized_name", SqlColumnType.NAME),
                    SqlColumn("first_seen_ms", SqlColumnType.BIGINT),
                    SqlColumn("last_seen_ms", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "player_uuid"),
            ),
            SqlTable(
                name = PLAYER_NAMES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("observed_name", SqlColumnType.NAME),
                    SqlColumn("normalized_name", SqlColumnType.NAME),
                    SqlColumn("first_seen_ms", SqlColumnType.BIGINT),
                    SqlColumn("last_seen_ms", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "player_uuid", "normalized_name"),
            ),
        )

    override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                "cz_players_name",
                PLAYERS,
                listOf("namespace", "normalized_name")
            ),
            SqlIndex(
                "cz_player_names_name",
                PLAYER_NAMES,
                listOf("namespace", "normalized_name")
            ),
        )

}
