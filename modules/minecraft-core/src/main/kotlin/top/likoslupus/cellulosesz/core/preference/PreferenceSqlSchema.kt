package top.likoslupus.cellulosesz.core.preference

import top.likoslupus.cellulosesz.foundation.database.schema.*

/**
 * The one shared table behind [PlayerPreference]. A single key/value row per player and preference
 * keeps every feature's toggle in the same shape, so the public API can expose them uniformly and a
 * new toggle never needs its own table.
 */
public object PreferenceSqlSchema : SqlSchemaContributor {

    public const val PREFERENCES: String = "cz_player_preferences"

    public override val id: String get() = "core.preference"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = PREFERENCES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("preference_key", SqlColumnType.NAME),
                    SqlColumn("preference_value", SqlColumnType.BOOLEAN),
                ),
                primaryKey = listOf("namespace", "player_uuid", "preference_key"),
            ),
        )

    public override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                name = "cz_player_preferences_key",
                table = PREFERENCES,
                columns = listOf("namespace", "preference_key"),
            ),
        )

}
