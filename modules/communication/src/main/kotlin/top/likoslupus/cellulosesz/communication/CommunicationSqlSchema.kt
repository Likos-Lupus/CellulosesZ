package top.likoslupus.cellulosesz.communication

import top.likoslupus.cellulosesz.foundation.database.schema.*

/** Communication tables. Ignore relationships are a real table, never a serialized JSON array. */
public object CommunicationSqlSchema : SqlSchemaContributor {

    public const val MESSAGING_PREFERENCES: String = "cz_messaging_preferences"
    public const val IGNORED_PLAYERS: String = "cz_ignored_players"
    public const val MAILBOXES: String = "cz_mailboxes"
    public const val MAIL_MESSAGES: String = "cz_mail_messages"

    /** Machine safety caps, deliberately far above the configurable product limits. */
    public const val MAX_STORED_IGNORES: Int = 10_000
    public const val MAX_STORED_MAIL_MESSAGES: Int = 10_000
    public const val MAX_STORED_MESSAGE_CHARS: Int = 16_384

    public override val id: String get() = "communication"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = MESSAGING_PREFERENCES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("player_name", SqlColumnType.NAME),
                    SqlColumn("receive_private_messages", SqlColumnType.BOOLEAN),
                    SqlColumn("reply_mode", SqlColumnType.NAME, nullable = true),
                ),
                primaryKey = listOf("namespace", "player_uuid"),
            ),
            SqlTable(
                name = IGNORED_PLAYERS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("owner_uuid", SqlColumnType.UUID),
                    SqlColumn("ignored_uuid", SqlColumnType.UUID),
                ),
                primaryKey = listOf("namespace", "owner_uuid", "ignored_uuid"),
            ),
            SqlTable(
                name = MAILBOXES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("owner_uuid", SqlColumnType.UUID),
                    SqlColumn("owner_name", SqlColumnType.NAME),
                ),
                primaryKey = listOf("namespace", "owner_uuid"),
            ),
            SqlTable(
                name = MAIL_MESSAGES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("message_id", SqlColumnType.UUID),
                    SqlColumn("owner_uuid", SqlColumnType.UUID),
                    SqlColumn("sender_type", SqlColumnType.NAME),
                    SqlColumn("sender_uuid", SqlColumnType.UUID, nullable = true),
                    SqlColumn("sender_name", SqlColumnType.NAME, nullable = true),
                    SqlColumn("body", SqlColumnType.TEXT),
                    SqlColumn("sent_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("expires_at_ms", SqlColumnType.BIGINT, nullable = true),
                    SqlColumn("read_at_ms", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "message_id"),
            ),
        )

    public override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                "cz_ignored_players_owner",
                IGNORED_PLAYERS,
                listOf("namespace", "owner_uuid")
            ),
            SqlIndex(
                "cz_mail_messages_owner",
                MAIL_MESSAGES,
                listOf("namespace", "owner_uuid", "sent_at_ms")
            ),
        )

}
