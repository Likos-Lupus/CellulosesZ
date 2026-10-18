package top.likoslupus.cellulosesz.economy.ledger

import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.economy.account.AccountKind
import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.money.Money
import java.sql.Connection
import java.sql.Types
import java.time.Instant
import java.util.*

/**
 * Writes the economy ledger through an already-open [Connection].
 *
 * The writer never opens a connection and never begins, commits or rolls back a transaction: it is
 * always called from inside the owner's JDBC boundary ([JdbcEconomyRepository]), so the guarded
 * balance updates, the transaction header and the balanced entries share one commit.
 */
internal class JdbcEconomyLedgerWriter(
    private val namespace: String,
    private val reader: JdbcEconomyLedgerReader,
) {

    /**
     * Posts one balanced ledger transaction: idempotency check, guarded balance updates, header and
     * one entry per line. A rejected line aborts the transaction with a typed [LedgerAbort].
     */
    fun postLedger(
        connection: Connection,
        header: LedgerHeader,
        lines: List<LedgerLine>,
        maximum: Money,
    ): LedgerCommitResult {
        reader.existingResult(connection, header)?.let { return it }

        val maximumMinor = maximum.toMinorUnitsExact()
        lines.forEach { line ->
            if (line.role == LedgerLineRole.SYSTEM_DEBIT
                || line.role == LedgerLineRole.SYSTEM_CREDIT
            ) {
                ensureAccount(
                    connection,
                    line.accountId,
                    header.createdAt
                )
            }
        }
        lines.forEach { line ->
            if (
                !guardedUpdate(
                    connection,
                    line,
                    maximumMinor,
                    header.createdAt
                )
            ) {
                classifyFailure(connection, line, maximumMinor)
            }
        }

        val transactionId = UUID.randomUUID()
        insertTransactionHeader(connection, transactionId, header, null)
        lines.forEachIndexed { ordinal, line ->
            val after = reader.readAccount(connection, line.accountId)?.balanceMinor
                ?: throw EconomyDataException("account vanished mid-transaction")
            insertEntry(
                connection,
                transactionId,
                ordinal,
                line.accountId,
                line.signedDelta,
                Money.fromMinorUnits(after)
            )
        }
        return LedgerCommitResult.Committed(reader.loadReceipt(connection, transactionId))
    }

    /**
     * Applies one line's guarded balance update. Returns false when no row matched; the caller then
     * reads the affected account to decide the exact typed reason instead of guessing.
     */
    fun guardedUpdate(
        connection: Connection,
        line: LedgerLine,
        maximumMinor: Long,
        at: Instant,
    ): Boolean {
        val amountMinor = line.amount.toMinorUnitsExact()
        val accountId = line.accountId.storageId
        val adjusted = when (line.role) {
            LedgerLineRole.CREDIT_PLAYER -> maximumMinor - amountMinor
            LedgerLineRole.SYSTEM_DEBIT -> Long.MIN_VALUE + amountMinor
            LedgerLineRole.SYSTEM_CREDIT -> Long.MAX_VALUE - amountMinor
            LedgerLineRole.DEBIT_PLAYER -> amountMinor
        }
        val kindFilter = when (line.role) {
            LedgerLineRole.DEBIT_PLAYER, LedgerLineRole.CREDIT_PLAYER -> "account_kind = 'PLAYER'"
            LedgerLineRole.SYSTEM_DEBIT -> "account_kind = 'MINT'"
            LedgerLineRole.SYSTEM_CREDIT -> "account_kind = 'SINK'"
        }
        val (operator, comparator) = when (line.role) {
            LedgerLineRole.DEBIT_PLAYER, LedgerLineRole.SYSTEM_DEBIT -> "-" to ">="
            LedgerLineRole.CREDIT_PLAYER, LedgerLineRole.SYSTEM_CREDIT -> "+" to "<="
        }
        return connection.prepareStatement(
            /* language=SQL */ """
            UPDATE
                ${EconomySqlSchema.ACCOUNTS}
            SET
                balance_minor = balance_minor $operator ?,
                revision = revision + 1,
                updated_at_ms = ?
            WHERE
                namespace = ?
                AND currency_id = ?
                AND account_id = ?
                AND $kindFilter
                AND balance_minor $comparator ?;
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, amountMinor)
            statement.setLong(2, at.toEpochMilli())
            statement.setString(3, namespace)
            statement.setString(4, CURRENCY_ID)
            statement.setString(5, accountId)
            statement.setLong(6, adjusted)
            statement.executeUpdate() > 0
        }
    }

    private fun classifyFailure(
        connection: Connection,
        line: LedgerLine,
        maximumMinor: Long,
    ): Nothing {
        val current = reader.readAccount(connection, line.accountId)
            ?: throw EconomyDataException("account '${line.accountId.storageId}' is missing")
        val amountMinor = line.amount.toMinorUnitsExact()
        when (line.role) {
            LedgerLineRole.DEBIT_PLAYER ->
                when {
                    current.balanceMinor < amountMinor ->
                        throw LedgerAbort(LedgerCommitResult.InsufficientFunds)

                    else ->
                        throw LedgerAbort(LedgerCommitResult.ConcurrentModification)
                }

            LedgerLineRole.CREDIT_PLAYER ->
                when {
                    maximumMinor - amountMinor < current.balanceMinor ->
                        throw LedgerAbort(LedgerCommitResult.BalanceLimitExceeded)

                    else ->
                        throw LedgerAbort(LedgerCommitResult.ConcurrentModification)
                }

            LedgerLineRole.SYSTEM_DEBIT,
            LedgerLineRole.SYSTEM_CREDIT ->
                throw LedgerAbort(LedgerCommitResult.BalanceLimitExceeded)
        }
    }

    fun updateBalanceCas(
        connection: Connection,
        account: EconomyAccountId,
        targetMinor: Long,
        expectedRevision: Long,
        at: Instant,
    ): Boolean =
        connection.prepareStatement(
            /* language=SQL */ """
            UPDATE
                ${EconomySqlSchema.ACCOUNTS}
            SET
                balance_minor = ?,
                revision = revision + 1,
                updated_at_ms = ?
            WHERE
                namespace = ?
                AND currency_id = ?
                AND account_id = ?
                AND account_kind = 'PLAYER'
                AND revision = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, targetMinor)
            statement.setLong(2, at.toEpochMilli())
            statement.setString(3, namespace)
            statement.setString(4, CURRENCY_ID)
            statement.setString(5, account.storageId)
            statement.setLong(6, expectedRevision)
            statement.executeUpdate() > 0
        }

    fun ensureAccount(
        connection: Connection,
        accountId: EconomyAccountId,
        at: Instant
    ): AccountRow {
        reader.readAccount(connection, accountId)?.let { return it }
        if (accountId.kind == AccountKind.PLAYER) {
            throw EconomyDataException("player economy account is missing: ${accountId.storageId}")
        }
        insertAccount(
            connection,
            accountId,
            accountId.kind,
            null,
            at
        )
        return AccountRow(
            accountId.kind,
            0L,
            0L
        )
    }

    fun insertAccount(
        connection: Connection,
        accountId: EconomyAccountId,
        kind: AccountKind,
        playerId: UUID?,
        at: Instant,
    ) {
        connection.prepareStatement(
            /* language=SQL */ """
            INSERT INTO ${EconomySqlSchema.ACCOUNTS} (
                namespace,
                currency_id,
                account_id,
                account_kind,
                player_uuid,
                balance_minor,
                revision,
                created_at_ms,
                updated_at_ms
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, CURRENCY_ID)
            statement.setString(3, accountId.storageId)
            statement.setString(4, kind.name)
            when (playerId) {
                null -> statement.setNull(5, Types.VARCHAR)
                else -> statement.setString(5, playerId.toString())
            }
            statement.setLong(6, 0L)
            statement.setLong(7, 0L)
            statement.setLong(8, at.toEpochMilli())
            statement.setLong(9, at.toEpochMilli())
            statement.executeUpdate()
        }
    }

    fun insertTransactionHeader(
        connection: Connection,
        transactionId: UUID,
        header: LedgerHeader,
        resultBalanceMinor: Long?,
    ) {
        connection.prepareStatement(
            /* language=SQL */ """
            INSERT INTO ${EconomySqlSchema.TRANSACTIONS} (
                namespace,
                transaction_id,
                currency_id,
                origin_id,
                request_key,
                request_hash,
                kind,
                actor_kind,
                actor_id,
                reason_code,
                reference_id,
                created_at_ms,
                result_balance_minor
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, transactionId.toString())
            statement.setString(3, CURRENCY_ID)
            statement.setString(4, header.originId)
            statement.setString(5, header.requestKey)
            statement.setString(6, header.requestHash)
            statement.setString(7, header.kind.name)
            statement.setString(8, header.actorKind.name)
            statement.setString(9, header.actorId)
            statement.setString(10, header.reasonCode)
            when (header.referenceId) {
                null -> statement.setNull(11, Types.VARCHAR)
                else -> statement.setString(11, header.referenceId)
            }
            statement.setLong(12, header.createdAt.toEpochMilli())
            when (resultBalanceMinor) {
                null -> statement.setNull(13, Types.BIGINT)
                else -> statement.setLong(13, resultBalanceMinor)
            }
            statement.executeUpdate()
        }
    }

    fun insertEntry(
        connection: Connection,
        transactionId: UUID,
        ordinal: Int,
        accountId: EconomyAccountId,
        delta: Money,
        balanceAfter: Money,
    ) {
        connection.prepareStatement(
            /* language=SQL */ """
            INSERT INTO ${EconomySqlSchema.ENTRIES} (
                namespace,
                transaction_id,
                entry_ordinal,
                currency_id,
                account_id,
                delta_minor,
                balance_after_minor
            )
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, transactionId.toString())
            statement.setInt(3, ordinal)
            statement.setString(4, CURRENCY_ID)
            statement.setString(5, accountId.storageId)
            statement.setLong(6, delta.toMinorUnitsExact())
            statement.setLong(7, balanceAfter.toMinorUnitsExact())
            statement.executeUpdate()
        }
    }

}
