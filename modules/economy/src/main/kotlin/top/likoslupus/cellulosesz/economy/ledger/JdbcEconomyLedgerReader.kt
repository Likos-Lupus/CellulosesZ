package top.likoslupus.cellulosesz.economy.ledger

import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.economy.account.AccountKind
import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.money.Money
import java.sql.Connection
import java.time.Instant
import java.util.*

/**
 * Reads the economy ledger through an already-open [Connection].
 *
 * The reader never opens a connection and never begins, commits or rolls back a transaction: it is
 * always called from inside the owner's JDBC boundary ([JdbcEconomyRepository]).
 */
internal class JdbcEconomyLedgerReader(
    private val namespace: String,
) {

    /**
     * Resolves the idempotent outcome of [header]: a matching committed transaction becomes an
     * [LedgerCommitResult.AlreadyCommitted], a different fingerprint at the same request key is an
     * [LedgerCommitResult.IdempotencyConflict], and an unseen request key returns `null`.
     */
    fun existingResult(connection: Connection, header: LedgerHeader): LedgerCommitResult? {
        val existing = readTransactionRow(
            connection,
            header.originId,
            header.requestKey
        ) ?: return null
        return when (existing.requestHash) {
            header.requestHash -> LedgerCommitResult.AlreadyCommitted(
                loadReceipt(connection, existing.transactionId)
            )

            else -> LedgerCommitResult.IdempotencyConflict
        }
    }

    fun readAccount(
        connection: Connection,
        accountId: EconomyAccountId
    ): AccountRow? =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                account_kind,
                balance_minor,
                revision
            FROM
                ${EconomySqlSchema.ACCOUNTS}
            WHERE
                namespace = ? AND currency_id = ? AND account_id = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, CURRENCY_ID)
            statement.setString(3, accountId.storageId)
            statement.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    AccountRow(
                        kind = parseAccountKind(rs.getString(1)),
                        balanceMinor = rs.getLong(2),
                        revision = rs.getLong(3),
                    )
                }
            }
        }

    fun readTransactionRow(
        connection: Connection,
        originId: String,
        requestKey: String,
    ): TransactionRow? =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                transaction_id,
                request_hash
            FROM
                ${EconomySqlSchema.TRANSACTIONS}
            WHERE
                namespace = ? AND origin_id = ? AND request_key = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, originId)
            statement.setString(3, requestKey)
            statement.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    TransactionRow(
                        UUID.fromString(rs.getString(1)),
                        rs.getString(2)
                    )
                }
            }
        }

    fun loadReceipt(connection: Connection, transactionId: UUID): EconomyReceipt {
        val header = connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                kind,
                currency_id,
                created_at_ms
            FROM
                ${EconomySqlSchema.TRANSACTIONS}
            WHERE
                namespace = ? AND transaction_id = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, transactionId.toString())
            statement.executeQuery().use { rs ->
                if (!rs.next()) {
                    throw EconomyDataException("transaction '$transactionId' has no header")
                }
                Triple(
                    parseTransactionKind(rs.getString(1)),
                    rs.getString(2),
                    Instant.ofEpochMilli(rs.getLong(3)),
                )
            }
        }
        val entries = connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                account_id,
                delta_minor,
                balance_after_minor
            FROM
                ${EconomySqlSchema.ENTRIES}
            WHERE
                namespace = ? AND transaction_id = ?
            ORDER BY
                entry_ordinal ASC;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, transactionId.toString())
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val accountId = EconomyAccountId.fromStorageId(rs.getString(1))
                            ?: throw EconomyDataException("transaction '$transactionId' references an unreadable account")
                        add(
                            RecordedEntry(
                                accountId = accountId,
                                delta = Money.fromMinorUnits(rs.getLong(2)),
                                balanceAfter = Money.fromMinorUnits(rs.getLong(3)),
                            )
                        )
                    }
                }
            }
        }
        return EconomyReceipt(
            transactionId = transactionId,
            kind = header.first,
            currencyId = header.second,
            at = header.third,
            entries = entries,
        )
    }

}

private fun parseAccountKind(raw: String): AccountKind =
    AccountKind.entries.firstOrNull { it.name == raw }
        ?: throw EconomyDataException("unknown economy account kind '$raw'")

internal fun parseTransactionKind(raw: String): TransactionKind =
    TransactionKind.entries.firstOrNull { it.name == raw }
        ?: throw EconomyDataException("unknown economy transaction kind '$raw'")
