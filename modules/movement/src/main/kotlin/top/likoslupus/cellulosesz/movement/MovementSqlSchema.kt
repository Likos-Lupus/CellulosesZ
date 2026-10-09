package top.likoslupus.cellulosesz.movement

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Movement tables and the position column layout shared by homes, warps, spawn and history. */
public object MovementSqlSchema : SqlSchemaContributor {

    public const val HOMES: String = "cz_homes"
    public const val WARPS: String = "cz_warps"
    public const val SPAWN: String = "cz_spawn"
    public const val TELEPORT_HISTORY: String = "cz_teleport_history"

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
        )

    public override fun indexes(): List<SqlIndex> =
        emptyList()

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
