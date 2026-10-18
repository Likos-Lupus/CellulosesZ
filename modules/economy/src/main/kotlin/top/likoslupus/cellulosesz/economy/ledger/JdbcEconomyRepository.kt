package top.likoslupus.cellulosesz.economy.ledger

import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.economy.account.AccountKind
import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types
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

/** How many times a whole transaction is attempted before a transient failure is surfaced. */
private const val MAX_COMMIT_ATTEMPTS: Int = 3

/** Base backoff between transient retries, multiplied by the attempt number and applied off the JDBC block. */
private const val BASE_BACKOFF_MS: Long = 25

/**
 * Recognizes the well-known *transient* backend failures (deadlock, serialization conflict, lock
 * wait timeout) that a retry of the whole transaction can resolve. Anything unrecognized is treated
 * as fatal so a genuine data or permission error is never retried.
 */
internal fun isTransientBackendFailure(exception: SQLException): Boolean {
    val state = exception.sqlState
    return when {
        // transaction rollback: serialization failure / deadlock (PostgreSQL, H2)
        state != null && state.startsWith("40") -> true
        else -> when (exception.errorCode) {
            // MySQL / MariaDB: deadlock, lock wait timeout
            1213, 1205 -> true
            // H2: lock timeout
            50200 -> true
            // SQLite: BUSY / LOCKED (including extended codes)
            else -> exception.errorCode and 0xFF in 5..6
        }
    }
}

/**
 * The single JDBC owner of the economy ledger. Every mutation runs inside one blocking
 * [DatabaseRuntime.transaction]: the guarded balance updates, the transaction header and the balanced
 * entries all commit together or roll back together. A missing account or an unreadable row is a
 * typed [EconomyDataException], never a silent zero balance.
 */
internal class JdbcEconomyRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : EconomyRepository {

    /** Thrown inside a transaction to force a rollback while returning a typed business result. */
    private class LedgerAbort(
        val result: LedgerCommitResult
    ) : RuntimeException(null, null, false, false)

    private class AccountRow(
        val kind: AccountKind,
        val balanceMinor: Long,
        val revision: Long
    )

    private class TransactionRow(
        val transactionId: UUID,
        val requestHash: String
    )

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
                    if (readAccount(connection, account) != null) {
                        AccountOpenResult.Existing
                    } else {
                        insertAccount(
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
                            postLedger(connection, header, lines, maximum)
                        }
                        AccountOpenResult.Opened
                    }
                }
            } catch (exception: SQLException) {
                val existing = try {
                    database.read { readAccount(it, account) }
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
                    readAccount(connection, accountId)?.let { row ->
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
                    postLedger(
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
                    existingResult(connection, header)?.let { return@transactionWithRetry it }

                    val row = readAccount(connection, account)
                        ?: throw EconomyDataException("player account missing for set: $playerId")
                    if (row.kind != AccountKind.PLAYER) {
                        throw EconomyDataException("account '${account.storageId}' is not a player account")
                    }

                    val targetMinor = target.toMinorUnitsExact()
                    val deltaMinor = targetMinor - row.balanceMinor
                    val transactionId = UUID.randomUUID()

                    if (deltaMinor == 0L) {
                        insertTransactionHeader(
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

                    val updated = updateBalanceCas(
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
                    ensureAccount(
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
                        !guardedUpdate(
                            connection,
                            counterpartLine,
                            0L,
                            header.createdAt
                        )
                    ) {
                        throw EconomyDataException("system account '${counterpart.storageId}' could not absorb a set adjustment")
                    }
                    val counterpartAfter = readAccount(
                        connection,
                        counterpart
                    )!!.balanceMinor

                    insertTransactionHeader(
                        connection,
                        transactionId,
                        header,
                        null
                    )
                    insertEntry(
                        connection,
                        transactionId,
                        0,
                        account,
                        Money.fromMinorUnits(deltaMinor),
                        target,
                    )
                    insertEntry(
                        connection,
                        transactionId,
                        1,
                        counterpart,
                        counterpartLine.signedDelta,
                        Money.fromMinorUnits(counterpartAfter),
                    )
                    LedgerCommitResult.Committed(
                        loadReceipt(
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
                    readTransactionRow(connection, originId, requestKey)?.transactionId
                }
            } catch (exception: SQLException) {
                throw EconomyDataException(
                    "unable to look up the economy transaction for '$originId/$requestKey'",
                    exception
                )
            }
        }

    private fun postLedger(
        connection: Connection,
        header: LedgerHeader,
        lines: List<LedgerLine>,
        maximum: Money,
    ): LedgerCommitResult {
        existingResult(connection, header)?.let { return it }

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
            val after = readAccount(connection, line.accountId)?.balanceMinor
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
        return LedgerCommitResult.Committed(loadReceipt(connection, transactionId))
    }

    private fun existingResult(connection: Connection, header: LedgerHeader): LedgerCommitResult? {
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

    /**
     * Applies one line's guarded balance update. Returns false when no row matched; the caller then
     * reads the affected account to decide the exact typed reason instead of guessing.
     */
    private fun guardedUpdate(
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
        val current = readAccount(connection, line.accountId)
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

    private fun updateBalanceCas(
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

    private fun ensureAccount(
        connection: Connection,
        accountId: EconomyAccountId,
        at: Instant
    ): AccountRow {
        readAccount(connection, accountId)?.let { return it }
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

    private fun insertAccount(
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

    private fun readAccount(
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

    private fun insertTransactionHeader(
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

    private fun insertEntry(
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

    private fun readTransactionRow(
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

    private fun loadReceipt(connection: Connection, transactionId: UUID): EconomyReceipt {
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

    private fun recoverExisting(
        header: LedgerHeader,
        exception: SQLException
    ): LedgerCommitResult {
        val existing = try {
            database.read {
                readTransactionRow(
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
                    database.read { loadReceipt(it, existing.transactionId) }
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

    private fun parseAccountKind(raw: String): AccountKind =
        AccountKind.entries.firstOrNull { it.name == raw }
            ?: throw EconomyDataException("unknown economy account kind '$raw'")

    private fun parseTransactionKind(raw: String): TransactionKind =
        TransactionKind.entries.firstOrNull { it.name == raw }
            ?: throw EconomyDataException("unknown economy transaction kind '$raw'")

}
