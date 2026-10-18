package top.likoslupus.cellulosesz.application

import top.likoslupus.cellulosesz.foundation.database.schema.*

/**
 * Operational state owned by the composition root rather than a feature: per-player command
 * cooldowns, which the command gate reads and writes but no single feature owns. The rows are
 * disposable (an expired row is pruned on load), so they never participate in a feature migration.
 */
public object ApplicationSqlSchema : SqlSchemaContributor {

    public const val COMMAND_COOLDOWNS: String = "cz_command_cooldowns"

    public override val id: String get() = "application"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = COMMAND_COOLDOWNS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("command", SqlColumnType.NAME),
                    SqlColumn("expires_at_ms", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "player_uuid", "command"),
            ),
        )

    public override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                name = "cz_command_cooldowns_expiry",
                table = COMMAND_COOLDOWNS,
                columns = listOf("namespace", "expires_at_ms"),
            ),
        )

}
