package top.likoslupus.cellulosesz.economy.ledger

import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.economy.account.AccountKind
import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The default origin for internal transactions such as account openings. */
internal const val INTERNAL_ORIGIN: String = "cellulosesz:internal"

/** The stable request fingerprint of the (single) opening transaction of an account. */
private const val OPENING_HASH: String = "opening"

/**
 * The single JDBC owner of the economy ledger. Every mutation runs inside one blocking
 * [DatabaseRuntime.transaction]: the guarded balance updates, the transaction header and the balanced
 * entries all commit together or roll back together. A missing account or an unreadable row is a
 * typed [EconomyDataException], never a silent zero balance.
 *
 * This class owns the transaction boundary and retry policy; the row-level SQL lives in
 * [JdbcEconomyLedgerWriter] and [JdbcEconomyLedgerReader], which only ever operate on the connection handed
 * to them here.
 */
internal class JdbcEconomyRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : EconomyRepository {

    private val reader = JdbcEconomyLedgerReader(namespace)
    private val writer = JdbcEconomyLedgerWriter(namespace, reader)

    /**
     * Runs one whole transaction, retrying a bounded number of times on a transient backend failure
     * with a short backoff **outside** the JDBC block. Each transaction begins with an idempotent
     * header check, so a retry is safe even if the previous attempt actually committed. A failure is
     * only retried while [isTransientBackendFailure] recognizes it; the request key and fingerprint
     * are never regenerated.
     */
    private suspend fun <T> transactionWithRetry(block: (Connection) -> T): T {
        var attempt = 0
        while (true) {
            try {
                return database.transaction(block)
            } catch (exception: SQLException) {
                attempt++
                if (attempt >= MAX_COMMIT_ATTEMPTS || !isTransientBackendFailure(exception)) {
                    throw exception
                }
                delay((BASE_BACKOFF_MS * attempt).milliseconds)
            }
        }
    }

    override suspend fun ensurePlayerAccount(
        playerId: UUID,
        openingAmount: Money,
        maximum: Money,
        at: Instant,
    ): AccountOpenResult =
        withContext(Dispatchers.IO) {
            val account = EconomyAccountId.Player(playerId)
            try {
                transactionWithRetry { connection ->
                    if (reader.readAccount(connection, account) != null) {
                        AccountOpenResult.Existing
                    } else {
                        writer.insertAccount(
                            connection,
                            account,
                            AccountKind.PLAYER,
                            playerId,
                            at
                        )
                        val openingMinor = openingAmount.toMinorUnitsExact()
                        if (openingMinor > 0L) {
                            val header = LedgerHeader(
                                kind = TransactionKind.OPENING,
                                originId = INTERNAL_ORIGIN,
                                requestKey = "open:$playerId",
                                requestHash = OPENING_HASH,
                                actorKind = ActorKind.INTERNAL,
                                actorId = "cellulosesz",
                                reasonCode = "economy.opening",
                                referenceId = null,
                                createdAt = at,
                            )
                            val lines = listOf(
                                LedgerLine(
                                    account,
                                    openingAmount,
                                    LedgerLineRole.CREDIT_PLAYER
                                ),
                                LedgerLine(
                                    EconomyAccountId.Mint,
                                    openingAmount,
                                    LedgerLineRole.SYSTEM_DEBIT
                                ),
                            )
                            writer.postLedger(connection, header, lines, maximum)
                        }
                        AccountOpenResult.Opened
                    }
                }
            } catch (exception: SQLException) {
                val existing = try {
                    database.read { reader.readAccount(it, account) }
                } catch (_: SQLException) {
                    null
                }
                if (existing != null) {
                    AccountOpenResult.Existing
                } else {
                    throw EconomyDataException(
                        "unable to open economy account for $playerId",
                        exception
                    )
                }
            }
        }

    override suspend fun account(accountId: EconomyAccountId): StoredAccountView? =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    reader.readAccount(connection, accountId)?.let { row ->
                        StoredAccountView(
                            accountId = accountId,
                            kind = row.kind,
                            balance = Money.fromMinorUnits(row.balanceMinor),
                            revision = row.revision,
                        )
                    }
                }
            } catch (exception: SQLException) {
                throw EconomyDataException(
                    "unable to read economy account '${accountId.storageId}'",
                    exception
                )
            }
        }

    override suspend fun commit(
        header: LedgerHeader,
        lines: List<LedgerLine>,
        maximum: Money,
    ): LedgerCommitResult =
        withContext(Dispatchers.IO) {
            require(lines.isNotEmpty()) { "a ledger transaction needs at least one entry" }
            val sum = lines.fold(Money.ZERO) { acc, line ->
                acc.plus(line.signedDelta)
            }
            require(sum.isZero) { "ledger transaction is not balanced: $sum" }
            try {
                transactionWithRetry { connection ->
                    writer.postLedger(
                        connection,
                        header,
                        lines,
                        maximum
                    )
                }
            } catch (abort: LedgerAbort) {
                abort.result
            } catch (exception: SQLException) {
                recoverExisting(header, exception)
            }
        }

    override suspend fun setBalance(
        header: LedgerHeader,
        playerId: UUID,
        target: Money,
    ): LedgerCommitResult =
        withContext(Dispatchers.IO) {
            val account = EconomyAccountId.Player(playerId)
            try {
                transactionWithRetry { connection ->
                    reader.existingResult(connection, header)
                            ?.let { return@transactionWithRetry it }

                    val row = reader.readAccount(connection, account)
                        ?: throw EconomyDataException("player account missing for set: $playerId")
                    if (row.kind != AccountKind.PLAYER) {
                        throw EconomyDataException("account '${account.storageId}' is not a player account")
                    }

                    val targetMinor = target.toMinorUnitsExact()
                    val deltaMinor = targetMinor - row.balanceMinor
                    val transactionId = UUID.randomUUID()

                    if (deltaMinor == 0L) {
                        writer.insertTransactionHeader(
                            connection,
                            transactionId,
                            header.copy(kind = TransactionKind.SET_NO_CHANGE),
                            targetMinor,
                        )
                        return@transactionWithRetry LedgerCommitResult.NoChange(
                            transactionId,
                            target
                        )
                    }

                    val updated = writer.updateBalanceCas(
                        connection,
                        account,
                        targetMinor,
                        row.revision,
                        header.createdAt
                    )
                    if (!updated) {
                        throw LedgerAbort(LedgerCommitResult.ConcurrentModification)
                    }

                    val counterpart = when {
                        deltaMinor > 0L -> EconomyAccountId.Mint
                        else -> EconomyAccountId.Sink
                    }
                    val counterpartRole = when {
                        deltaMinor > 0L -> LedgerLineRole.SYSTEM_DEBIT
                        else -> LedgerLineRole.SYSTEM_CREDIT
                    }
                    val magnitude = Money.fromMinorUnits(
                        when {
                            deltaMinor > 0L -> deltaMinor
                            else -> -deltaMinor
                        }
                    )
                    writer.ensureAccount(
                        connection,
                        counterpart,
                        header.createdAt
                    )
                    val counterpartLine = LedgerLine(
                        counterpart,
                        magnitude,
                        counterpartRole
                    )
                    if (
                        !writer.guardedUpdate(
                            connection,
                            counterpartLine,
                            0L,
                            header.createdAt
                        )
                    ) {
                        throw EconomyDataException("system account '${counterpart.storageId}' could not absorb a set adjustment")
                    }
                    val counterpartAfter = reader.readAccount(
                        connection,
                        counterpart
                    )!!.balanceMinor

                    writer.insertTransactionHeader(
                        connection,
                        transactionId,
                        header,
                        null
                    )
                    writer.insertEntry(
                        connection,
                        transactionId,
                        0,
                        account,
                        Money.fromMinorUnits(deltaMinor),
                        target,
                    )
                    writer.insertEntry(
                        connection,
                        transactionId,
                        1,
                        counterpart,
                        counterpartLine.signedDelta,
                        Money.fromMinorUnits(counterpartAfter),
                    )
                    LedgerCommitResult.Committed(
                        reader.loadReceipt(
                            connection,
                            transactionId
                        )
                    )
                }
            } catch (abort: LedgerAbort) {
                abort.result
            } catch (exception: SQLException) {
                recoverExisting(header, exception)
            }
        }

    override suspend fun history(
        playerId: UUID,
        offset: Int,
        limit: Int
    ): HistoryPage =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    val accountId = EconomyAccountId.Player(playerId).storageId
                    val total = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            COUNT(*)
                        FROM
                            ${EconomySqlSchema.ENTRIES}
                        WHERE
                            namespace = ? AND currency_id = ? AND account_id = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, CURRENCY_ID)
                        statement.setString(3, accountId)
                        statement.executeQuery().use { rs ->
                            rs.next()
                            rs.getInt(1)
                        }
                    }
                    val entries = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            e.transaction_id,
                            e.delta_minor,
                            e.balance_after_minor,
                            t.kind,
                            t.created_at_ms,
                            t.origin_id,
                            t.reason_code,
                            t.reference_id
                        FROM
                            ${EconomySqlSchema.ENTRIES} e
                        JOIN ${EconomySqlSchema.TRANSACTIONS} t
                            ON e.namespace = t.namespace AND e.transaction_id = t.transaction_id
                        WHERE
                            e.namespace = ? AND e.currency_id = ? AND e.account_id = ?
                        ORDER BY
                            t.created_at_ms DESC, e.transaction_id DESC
                        LIMIT ? OFFSET ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, CURRENCY_ID)
                        statement.setString(3, accountId)
                        statement.setInt(4, limit)
                        statement.setInt(5, offset)
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        HistoryEntry(
                                            transactionId = UUID.fromString(rs.getString(1)),
                                            delta = Money.fromMinorUnits(rs.getLong(2)),
                                            balanceAfter = Money.fromMinorUnits(rs.getLong(3)),
                                            kind = parseTransactionKind(rs.getString(4)),
                                            at = Instant.ofEpochMilli(rs.getLong(5)),
                                            originId = rs.getString(6),
                                            reasonCode = rs.getString(7),
                                            referenceId = rs.getString(8),
                                        )
                                    )
                                }
                            }
                        }
                    }
                    HistoryPage(entries, total)
                }
            } catch (exception: SQLException) {
                throw EconomyDataException(
                    "unable to read economy history for $playerId",
                    exception
                )
            }
        }

    override suspend fun top(offset: Int, limit: Int, includeZero: Boolean): TopPage =
        withContext(Dispatchers.IO) {
            val balanceFilter = if (includeZero) "balance_minor >= 0" else "balance_minor > 0"
            try {
                database.read { connection ->
                    val total = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            COUNT(*)
                        FROM
                            ${EconomySqlSchema.ACCOUNTS}
                        WHERE
                            namespace = ?
                            AND currency_id = ?
                            AND account_kind = 'PLAYER'
                            AND $balanceFilter;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, CURRENCY_ID)
                        statement.executeQuery().use { rs ->
                            rs.next()
                            rs.getInt(1)
                        }
                    }
                    val entries = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            account_id,
                            balance_minor
                        FROM
                            ${EconomySqlSchema.ACCOUNTS}
                        WHERE
                            namespace = ?
                            AND currency_id = ?
                            AND account_kind = 'PLAYER'
                            AND $balanceFilter
                        ORDER BY
                            balance_minor DESC, account_id ASC
                        LIMIT ? OFFSET ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, CURRENCY_ID)
                        statement.setInt(3, limit)
                        statement.setInt(4, offset)
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    val id = EconomyAccountId.fromStorageId(rs.getString(1))
                                    if (id !is EconomyAccountId.Player) {
                                        throw EconomyDataException("ranking row is not a player account")
                                    }
                                    add(
                                        TopEntry(
                                            id.playerUuid,
                                            Money.fromMinorUnits(rs.getLong(2))
                                        )
                                    )
                                }
                            }
                        }
                    }
                    TopPage(entries, total)
                }
            } catch (exception: SQLException) {
                throw EconomyDataException("unable to read the economy ranking", exception)
            }
        }

    // ---- transaction internals -------------------------------------------------------------

    override suspend fun transactionId(originId: String, requestKey: String): UUID? =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    reader.readTransactionRow(connection, originId, requestKey)?.transactionId
                }
            } catch (exception: SQLException) {
                throw EconomyDataException(
                    "unable to look up the economy transaction for '$originId/$requestKey'",
                    exception
                )
            }
        }

    private fun recoverExisting(
        header: LedgerHeader,
        exception: SQLException
    ): LedgerCommitResult {
        val existing = try {
            database.read {
                reader.readTransactionRow(
                    it,
                    header.originId,
                    header.requestKey
                )
            }
        } catch (_: SQLException) {
            null
        } ?: throw EconomyDataException("unable to commit economy transaction", exception)
        return if (existing.requestHash == header.requestHash) {
            LedgerCommitResult.AlreadyCommitted(
                try {
                    database.read { reader.loadReceipt(it, existing.transactionId) }
                } catch (readException: SQLException) {
                    throw EconomyDataException(
                        "unable to read the committed economy transaction",
                        readException
                    )
                }
            )
        } else {
            LedgerCommitResult.IdempotencyConflict
        }
    }

}
