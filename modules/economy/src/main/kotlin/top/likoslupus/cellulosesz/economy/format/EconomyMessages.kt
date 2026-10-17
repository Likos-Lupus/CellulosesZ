package top.likoslupus.cellulosesz.economy.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.economy.integration.EconomyTransactionResult
import top.likoslupus.cellulosesz.economy.money.Money
import java.math.BigDecimal

/** The currency label used in player-facing amounts; only the display name is configurable. */
internal class EconomyDisplay(
    val currencyName: String,
)

/** One rendered ranking line. */
internal class TopRow(
    val rank: Int,
    val name: String,
    val amount: BigDecimal,
)

/** One rendered history line. */
internal class HistoryRow(
    val kind: String,
    val delta: BigDecimal,
    val balanceAfter: BigDecimal,
)

/**
 * Maps economy domain results to semantic values. It holds no language and no colour: the
 * presentation layer renders per recipient.
 */
internal object EconomyMessages {

    fun balanceSelf(amount: BigDecimal, display: EconomyDisplay): Notice =
        success(
            key("economy.balance.self"),
            money(amount, display)
        )

    fun balanceOther(
        name: String,
        amount: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        info(
            key("economy.balance.other"),
            playerName(name),
            money(amount, display)
        )

    fun unknownPlayer(name: String): Notice =
        notice(
            key("economy.pay.error.unknown_player"),
            playerName(name)
        )

    fun disabled(): Notice =
        notice(key("economy.disabled"))

    fun storageUnavailable(): Notice =
        notice(key("economy.storage.unavailable"))

    fun paySuccessSender(
        receiver: String,
        amount: BigDecimal,
        balance: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        success(
            key("economy.pay.success.sender"),
            playerName(receiver),
            money(amount, display),
            money(balance, display),
        )

    fun paySuccessReceiver(
        sender: String,
        amount: BigDecimal,
        balance: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        success(
            key("economy.pay.success.receiver"),
            playerName(sender),
            money(amount, display),
            money(balance, display),
        )

    fun ecoGrant(
        player: String,
        amount: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        success(
            key("economy.eco.grant.success"),
            money(amount, display),
            playerName(player)
        )

    fun ecoTake(
        player: String,
        amount: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        success(
            key("economy.eco.take.success"),
            money(amount, display),
            playerName(player)
        )

    fun ecoSet(
        player: String,
        target: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        success(
            key("economy.eco.set.success"),
            playerName(player),
            money(target, display)
        )

    fun ecoSetNoChange(
        player: String,
        target: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        info(
            key("economy.eco.set.no_change"),
            playerName(player),
            money(target, display)
        )

    fun ecoInfo(
        currencyName: String,
        maximum: BigDecimal,
        display: EconomyDisplay
    ): Notice =
        info(
            key("economy.eco.info"),
            literal(currencyName),
            money(maximum, display)
        )

    /** Maps a transaction result to its failure notice; callers handle the success branches. */
    fun transactionFailure(result: EconomyTransactionResult): Notice =
        notice(
            key(
                when (result) {
                    is EconomyTransactionResult.Committed,
                    is EconomyTransactionResult.AlreadyCommitted,
                    is EconomyTransactionResult.NoChange -> "economy.eco.error"

                    EconomyTransactionResult.InsufficientFunds -> "economy.pay.error.insufficient"
                    EconomyTransactionResult.ExceedsBalanceLimit -> "economy.pay.error.balance_limit"
                    EconomyTransactionResult.InvalidAmount -> "economy.pay.error.invalid_amount"
                    EconomyTransactionResult.TransferLimitExceeded -> "economy.pay.error.amount_limit"
                    EconomyTransactionResult.SameAccount -> "economy.pay.error.same_player"
                    EconomyTransactionResult.IdempotencyConflict -> "economy.pay.error.conflict"
                    EconomyTransactionResult.Disabled -> "economy.disabled"
                    EconomyTransactionResult.StorageUnavailable -> "economy.storage.unavailable"
                }
            )
        )

    fun historyPage(
        rows: List<HistoryRow>,
        page: Int,
        pageSize: Int,
        total: Int,
        display: EconomyDisplay,
    ): ListPage =
        ListPage(
            title = LocalizedText(
                key("economy.transactions.title"),
                listOf(count(total.toLong()))
            ),
            rows = rows.map { row ->
                ListRow(
                    main = LocalizedText(
                        key("economy.transactions.row"),
                        listOf(
                            literal(row.kind),
                            money(row.delta, display),
                            money(row.balanceAfter, display),
                        ),
                    )
                )
            },
            emptyText = LocalizedText(key("economy.transactions.empty")),
        )

    fun topPage(
        rows: List<TopRow>,
        page: Int,
        pageSize: Int,
        total: Int,
        display: EconomyDisplay,
    ): ListPage =
        ListPage(
            title = LocalizedText(
                key("economy.top.title"),
                listOf(count(total.toLong()))
            ),
            rows = rows.map { row ->
                ListRow(
                    main = LocalizedText(
                        key("economy.top.row"),
                        listOf(
                            count(row.rank.toLong()),
                            playerName(row.name),
                            money(row.amount, display)
                        ),
                    )
                )
            },
            emptyText = LocalizedText(key("economy.top.empty")),
        )

    // ---- helpers ---------------------------------------------------------------------------

    private fun key(value: String): MessageKey =
        MessageKey(value)

    private fun notice(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.ERROR
        )

    private fun success(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.SUCCESS
        )

    private fun info(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.INFO
        )

    private fun literal(value: String): TextArgument =
        TextArgument.Literal(value)

    private fun playerName(value: String): TextArgument =
        TextArgument.PlayerName(value)

    private fun count(value: Long): TextArgument =
        TextArgument.Count(value)

    private fun money(amount: BigDecimal, display: EconomyDisplay): TextArgument =
        TextArgument.Literal("${amount.toPlainString()} ${display.currencyName}")

    private fun money(amount: Money, display: EconomyDisplay): TextArgument =
        money(amount.decimal, display)

}
