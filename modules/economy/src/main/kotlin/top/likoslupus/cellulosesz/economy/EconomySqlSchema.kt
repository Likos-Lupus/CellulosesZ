package top.likoslupus.cellulosesz.economy

import top.likoslupus.cellulosesz.foundation.database.schema.*

/**
 * The native economy ledger: current account balances, committed transaction headers and their
 * balanced entries. Table order is `accounts -> transactions -> entries` so a future foreign-key
 * pass can reference earlier tables.
 */
public object EconomySqlSchema : SqlSchemaContributor {

    public const val ACCOUNTS: String = "cz_economy_accounts"
    public const val TRANSACTIONS: String = "cz_economy_transactions"
    public const val ENTRIES: String = "cz_economy_entries"
    public const val WORTH: String = "cz_economy_worth"
    public const val SALE_INTENTS: String = "cz_economy_sale_intents"

    public override val id: String
        get() = "economy"

    public override fun tables(): List<SqlTable> =
        listOf(
            SqlTable(
                name = ACCOUNTS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("currency_id", SqlColumnType.RESOURCE),
                    SqlColumn("account_id", SqlColumnType.RESOURCE),
                    SqlColumn("account_kind", SqlColumnType.NAME),
                    SqlColumn("player_uuid", SqlColumnType.UUID, nullable = true),
                    SqlColumn("balance_minor", SqlColumnType.BIGINT),
                    SqlColumn("revision", SqlColumnType.BIGINT),
                    SqlColumn("created_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("updated_at_ms", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "currency_id", "account_id"),
            ),
            SqlTable(
                name = TRANSACTIONS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("transaction_id", SqlColumnType.UUID),
                    SqlColumn("currency_id", SqlColumnType.RESOURCE),
                    SqlColumn("origin_id", SqlColumnType.RESOURCE),
                    SqlColumn("request_key", SqlColumnType.RESOURCE),
                    SqlColumn("request_hash", SqlColumnType.HASH),
                    SqlColumn("kind", SqlColumnType.NAME),
                    SqlColumn("actor_kind", SqlColumnType.NAME),
                    SqlColumn("actor_id", SqlColumnType.RESOURCE),
                    SqlColumn("reason_code", SqlColumnType.RESOURCE),
                    SqlColumn("reference_id", SqlColumnType.RESOURCE, nullable = true),
                    SqlColumn("created_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("result_balance_minor", SqlColumnType.BIGINT, nullable = true),
                ),
                primaryKey = listOf("namespace", "transaction_id"),
            ),
            SqlTable(
                name = ENTRIES,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("transaction_id", SqlColumnType.UUID),
                    SqlColumn("entry_ordinal", SqlColumnType.INTEGER),
                    SqlColumn("currency_id", SqlColumnType.RESOURCE),
                    SqlColumn("account_id", SqlColumnType.RESOURCE),
                    SqlColumn("delta_minor", SqlColumnType.BIGINT),
                    SqlColumn("balance_after_minor", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "transaction_id", "entry_ordinal"),
            ),
            SqlTable(
                name = WORTH,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("item_id", SqlColumnType.RESOURCE),
                    SqlColumn("price_minor", SqlColumnType.BIGINT),
                    SqlColumn("updated_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("updated_by", SqlColumnType.NAME, nullable = true),
                ),
                primaryKey = listOf("namespace", "item_id"),
            ),
            SqlTable(
                name = SALE_INTENTS,
                columns = listOf(
                    SqlColumn("namespace", SqlColumnType.NAME),
                    SqlColumn("sale_id", SqlColumnType.UUID),
                    SqlColumn("player_uuid", SqlColumnType.UUID),
                    SqlColumn("player_name", SqlColumnType.NAME),
                    SqlColumn("state", SqlColumnType.NAME),
                    SqlColumn("item_id", SqlColumnType.RESOURCE),
                    SqlColumn("item_count", SqlColumnType.INTEGER),
                    SqlColumn("total_minor", SqlColumnType.BIGINT),
                    SqlColumn("payload", SqlColumnType.TEXT),
                    SqlColumn("transaction_id", SqlColumnType.UUID, nullable = true),
                    SqlColumn("created_at_ms", SqlColumnType.BIGINT),
                    SqlColumn("updated_at_ms", SqlColumnType.BIGINT),
                ),
                primaryKey = listOf("namespace", "sale_id"),
            ),
        )

    public override fun indexes(): List<SqlIndex> =
        listOf(
            SqlIndex(
                name = "cz_economy_accounts_top",
                table = ACCOUNTS,
                columns = listOf(
                    "namespace",
                    "currency_id",
                    "account_kind",
                    "balance_minor",
                    "account_id",
                ),
            ),
            SqlIndex(
                name = "cz_economy_request_unique",
                table = TRANSACTIONS,
                columns = listOf("namespace", "origin_id", "request_key"),
                unique = true,
            ),
            SqlIndex(
                name = "cz_economy_transactions_time",
                table = TRANSACTIONS,
                columns = listOf("namespace", "currency_id", "created_at_ms", "transaction_id"),
            ),
            SqlIndex(
                name = "cz_economy_entries_account",
                table = ENTRIES,
                columns = listOf("namespace", "currency_id", "account_id", "transaction_id"),
            ),
            SqlIndex(
                name = "cz_economy_sale_intents_state",
                table = SALE_INTENTS,
                columns = listOf("namespace", "state", "created_at_ms"),
            ),
        )

}
