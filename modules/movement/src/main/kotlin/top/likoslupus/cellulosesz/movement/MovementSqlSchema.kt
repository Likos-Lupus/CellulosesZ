package top.likoslupus.cellulosesz.movement

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Movement tables and the position column layout shared by homes, warps, spawn and history. */
public object MovementSqlSchema : SqlSchemaContributor {

    public const val HOMES: String = "cz_homes"
    public const val WARPS: String = "cz_warps"
    public const val SPAWN: String = "cz_spawn"
    public const val TELEPORT_HISTORY: String = "cz_teleport_history"
    public const val RANDOM_TELEPORT_REGIONS: String = "cz_random_teleport_regions"
    public const val WARP_METADATA: String = "cz_warp_metadata"
    public const val TPAUTO_PREFERENCES: String = "cz_tpauto_preferences"
    public const val TPAUTO_ALLOWED: String = "cz_tpauto_allowed"

    public override val id: String get() = "movement"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = HOMES,
                columns = buildList {
                    add(SqlColumn("namespace", SqlColumnType.NAME))
                    add(SqlColumn("owner_uuid", SqlColumnType.UUID))
                    add(SqlColumn("name", SqlColumnType.NAME))
                    addAll(positionColumns())
                },
                primaryKey = listOf("namespace", "owner_uuid", "name"),
            ),
            SqlTable(
                name = WARPS,
                columns = buildList {
                    add(SqlColumn("namespace", SqlColumnType.NAME))
                    add(SqlColumn("name", SqlColumnType.NAME))
                    addAll(positionColumns())
                },
                primaryKey = listOf("namespace", "name"),
            ),
            SqlTable(
                name = SPAWN,
                columns = buildList {
                    add(SqlColumn("namespace", SqlColumnType.NAME))
                    addAll(positionColumns())
                },
                primaryKey = listOf("namespace"),
            ),
            SqlTable(
                name = TELEPORT_HISTORY,
                columns = buildList {
                    add(SqlColumn("namespace", SqlColumnType.NAME))
                    add(SqlColumn("player_uuid", SqlColumnType.UUID))
                    addAll(positionColumns())
                },
                primaryKey = listOf("namespace", "player_uuid"),
            ),
            SqlTable(
                name = RANDOM_TELEPORT_REGIONS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("name", SqlColumnType.NAME),
                    SqlColumn("dimension", SqlColumnType.RESOURCE),
                    SqlColumn("center_x", SqlColumnType.DOUBLE),
                    SqlColumn("center_z", SqlColumnType.DOUBLE),
                    SqlColumn("minimum_radius", SqlColumnType.DOUBLE),
                    SqlColumn("maximum_radius", SqlColumnType.DOUBLE),
                    SqlColumn("enabled", SqlColumnType.BOOLEAN),
                    SqlColumn("revision", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "name"),
            ),
            SqlTable(
                name = WARP_METADATA,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("warp_name", SqlColumnType.NAME),
                    SqlColumn("created_by", SqlColumnType.TEXT),
                    SqlColumn("created_at", SqlColumnType.BIGINT),
                    SqlColumn("updated_at_ms", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "warp_name"),
            ),
            SqlTable(
                name = TPAUTO_PREFERENCES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("mode", SqlColumnType.NAME),
                ),
                primaryKey = listOf("namespace", "player_uuid"),
            ),
            SqlTable(
                name = TPAUTO_ALLOWED,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("owner_uuid", SqlColumnType.UUID),
                    SqlColumn("allowed_uuid", SqlColumnType.UUID),
                ),
                primaryKey = listOf("namespace", "owner_uuid", "allowed_uuid"),
            ),
        )

    public override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                "cz_random_teleport_regions_dimension",
                RANDOM_TELEPORT_REGIONS,
                listOf("namespace", "dimension"),
            ),
        )

    private fun positionColumns(): List<SqlColumn> =
        listOf(
            SqlColumn("dimension", SqlColumnType.RESOURCE),
            SqlColumn("x", SqlColumnType.DOUBLE),
            SqlColumn("y", SqlColumnType.DOUBLE),
            SqlColumn("z", SqlColumnType.DOUBLE),
            SqlColumn("yaw", SqlColumnType.REAL),
            SqlColumn("pitch", SqlColumnType.REAL),
        )

}
