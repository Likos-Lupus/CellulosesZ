package top.likoslupus.cellulosesz.core.text.i18n.preference

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Explicit player language overrides. Kept separate from the identity index: different concern. */
public object LocalizationSqlSchema : SqlSchemaContributor {

    public const val PLAYER_LANGUAGES: String = "cz_player_languages"

    override val id: String
        get() = "localization"

    override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = PLAYER_LANGUAGES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("language", SqlColumnType.NAME),
                ),
                primaryKey = listOf("namespace", "player_uuid"),
            )
        )

    override fun indexes(): List<SqlIndex> =
        emptyList()

}
