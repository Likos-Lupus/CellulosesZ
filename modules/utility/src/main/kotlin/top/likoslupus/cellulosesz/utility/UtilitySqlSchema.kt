package top.likoslupus.cellulosesz.utility

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Utility tables: the kit catalog, its item payloads, per-player claims, and system grants. */
public object UtilitySqlSchema : SqlSchemaContributor {

    public const val KITS: String = "cz_kits"
    public const val KIT_ITEMS: String = "cz_kit_items"
    public const val KIT_CLAIMS: String = "cz_kit_claims"
    public const val KIT_GRANTS: String = "cz_kit_grants"
    public const val POWERTOOLS: String = "cz_powertools"
    public const val UNLIMITED_ITEMS: String = "cz_unlimited_items"

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
            SqlTable(
                name = KIT_GRANTS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("addon_id", SqlColumnType.RESOURCE),
                    SqlColumn("grant_id", SqlColumnType.RESOURCE),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("kit_name", SqlColumnType.NAME),
                    SqlColumn("state", SqlColumnType.NAME),
                    SqlColumn("created_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("delivered_at_ms", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "addon_id", "grant_id", "player_uuid"),
            ),
            SqlTable(
                name = POWERTOOLS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("item_id", SqlColumnType.RESOURCE),
                    SqlColumn("trigger", SqlColumnType.NAME),
                    SqlColumn("command", SqlColumnType.TEXT),
                    SqlColumn("updated_at_ms", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "player_uuid", "item_id", "trigger"),
            ),
            SqlTable(
                name = UNLIMITED_ITEMS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("item_id", SqlColumnType.RESOURCE),
                ),
                primaryKey = listOf("namespace", "player_uuid", "item_id"),
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
            SqlIndex(
                name = "cz_kit_grants_player",
                table = KIT_GRANTS,
                columns = listOf("namespace", "player_uuid")
            ),
        )

}
