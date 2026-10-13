package top.likoslupus.cellulosesz.administration

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Administration tables. Native account/IP bans stay in vanilla and are never mirrored here. */
public object AdministrationSqlSchema : SqlSchemaContributor {

    public const val MUTES: String = "cz_mutes"
    public const val MODERATION_AUDIT: String = "cz_moderation_audit"
    public const val MODERATION_AUDIT_DETAILS: String = "cz_moderation_audit_details"
    public const val JAIL_LOCATIONS: String = "cz_jail_locations"
    public const val JAIL_SENTENCES: String = "cz_jail_sentences"

    public override val id: String get() = "administration"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = MUTES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("player_name", SqlColumnType.NAME),
                    SqlColumn("actor_type", SqlColumnType.NAME),
                    SqlColumn("actor_uuid", SqlColumnType.UUID, nullable = true),
                    SqlColumn("actor_name", SqlColumnType.NAME),
                    SqlColumn("reason", SqlColumnType.TEXT),
                    SqlColumn("issued_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("expires_at_ms", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "player_uuid"),
            ),
            SqlTable(
                name = MODERATION_AUDIT,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("audit_id", SqlColumnType.UUID),
                    SqlColumn("occurred_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("actor_type", SqlColumnType.NAME),
                    SqlColumn("actor_uuid", SqlColumnType.UUID, nullable = true),
                    SqlColumn("actor_name", SqlColumnType.NAME),
                    SqlColumn("action", SqlColumnType.NAME),
                    SqlColumn("target_type", SqlColumnType.NAME),
                    SqlColumn("target_id", SqlColumnType.RESOURCE, nullable = true),
                    SqlColumn("target_name", SqlColumnType.NAME),
                    SqlColumn("reason", SqlColumnType.TEXT, nullable = true),
                    SqlColumn("expires_at_ms", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "audit_id"),
            ),
            SqlTable(
                name = MODERATION_AUDIT_DETAILS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("audit_id", SqlColumnType.UUID),
                    SqlColumn("detail_key", SqlColumnType.NAME),
                    SqlColumn("detail_value", SqlColumnType.TEXT),
                ),
                primaryKey = listOf("namespace", "audit_id", "detail_key"),
            ),
            SqlTable(
                name = JAIL_LOCATIONS,
                columns = buildList {
                    add(SqlColumn("namespace", SqlColumnType.NAME))
                    add(SqlColumn("name", SqlColumnType.NAME))
                    addAll(positionColumns())
                    add(SqlColumn("radius", SqlColumnType.DOUBLE))
                    add(SqlColumn("created_at_ms", SqlColumnType.BIGINT))
                    add(SqlColumn("created_by", SqlColumnType.NAME, nullable = true))
                },
                primaryKey = listOf("namespace", "name"),
            ),
            SqlTable(
                name = JAIL_SENTENCES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("player_name", SqlColumnType.NAME),
                    SqlColumn("jail_name", SqlColumnType.NAME),
                    SqlColumn("status", SqlColumnType.NAME),
                    SqlColumn("issued_by", SqlColumnType.NAME),
                    SqlColumn("reason", SqlColumnType.TEXT),
                    SqlColumn("issued_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("expires_at_ms", SqlColumnType.BIGINT, nullable = true),
                    SqlColumn("return_dimension", SqlColumnType.NAME, nullable = true),
                    SqlColumn("return_x", SqlColumnType.DOUBLE, nullable = true),
                    SqlColumn("return_y", SqlColumnType.DOUBLE, nullable = true),
                    SqlColumn("return_z", SqlColumnType.DOUBLE, nullable = true),
                    SqlColumn("return_yaw", SqlColumnType.REAL, nullable = true),
                    SqlColumn("return_pitch", SqlColumnType.REAL, nullable = true),
                    SqlColumn("release_requested_at_ms", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "player_uuid"),
            ),
        )

    public override fun indexes(): List<SqlIndex> = listOf(
        SqlIndex(
            "cz_moderation_audit_time",
            MODERATION_AUDIT,
            listOf("namespace", "occurred_at_ms")
        ),
        SqlIndex(
            "cz_moderation_audit_target",
            MODERATION_AUDIT,
            listOf("namespace", "target_id", "occurred_at_ms")
        ),
        SqlIndex(
            "cz_jail_sentences_status_expiry",
            JAIL_SENTENCES,
            listOf("namespace", "status", "expires_at_ms")
        ),
        SqlIndex(
            "cz_jail_sentences_jail",
            JAIL_SENTENCES,
            listOf("namespace", "jail_name")
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
