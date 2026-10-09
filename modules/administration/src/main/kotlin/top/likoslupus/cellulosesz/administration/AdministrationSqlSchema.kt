package top.likoslupus.cellulosesz.administration

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Administration tables. Native account/IP bans stay in vanilla and are never mirrored here. */
public object AdministrationSqlSchema : SqlSchemaContributor {

    public const val MUTES: String = "cz_mutes"
    public const val MODERATION_AUDIT: String = "cz_moderation_audit"
    public const val MODERATION_AUDIT_DETAILS: String = "cz_moderation_audit_details"

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
    )

}
