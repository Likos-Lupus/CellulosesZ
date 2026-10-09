package top.likoslupus.cellulosesz.utility

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Utility tables: the kit catalog, its item payloads, and per-player claims. */
public object UtilitySqlSchema : SqlSchemaContributor {

    public const val KITS: String = "cz_kits"
    public const val KIT_ITEMS: String = "cz_kit_items"
    public const val KIT_CLAIMS: String = "cz_kit_claims"

    public override val id: String
        get() = "utility"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = KITS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("kit_id", SqlColumnType.UUID),
                    SqlColumn("name", SqlColumnType.NAME),
                    SqlColumn("reuse_mode", SqlColumnType.NAME),
                    SqlColumn("cooldown_seconds", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "kit_id"),
            ),
            SqlTable(
                name = KIT_ITEMS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("kit_id", SqlColumnType.UUID),
                    SqlColumn("ordinal", SqlColumnType.INTEGER),
                    SqlColumn("payload", SqlColumnType.TEXT),
                ),
                primaryKey = listOf("namespace", "kit_id", "ordinal"),
            ),
            SqlTable(
                name = KIT_CLAIMS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("player_name", SqlColumnType.NAME),
                    SqlColumn("kit_name", SqlColumnType.NAME),
                    SqlColumn("kit_id", SqlColumnType.UUID),
                    SqlColumn("claimed_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("status", SqlColumnType.NAME),
                ),
                primaryKey = listOf("namespace", "player_uuid", "kit_name"),
            ),
        )

    public override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                name = "cz_kits_name_unique",
                table = KITS,
                columns = listOf("namespace", "name"),
                unique = true
            ),
            SqlIndex(
                name = "cz_kit_claims_player",
                table = KIT_CLAIMS,
                columns = listOf("namespace", "player_uuid")
            ),
        )

}
