package top.likoslupus.cellulosesz.economy.command

import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.format.EconomyDisplay
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.format.HistoryRow
import top.likoslupus.cellulosesz.economy.format.TopRow
import top.likoslupus.cellulosesz.economy.integration.*
import top.likoslupus.cellulosesz.economy.money.MoneyParsing
import java.math.BigDecimal
import java.util.*

/**
 * Every economy command. Each resolves its target on the server thread, builds an authorized
 * immutable request, and hands it to the single [EconomyIntegrationPort]; none writes a balance
 * directly and none blocks the server thread on JDBC.
 */
internal object EconomyCommands {

    private const val ORIGIN = "cellulosesz:command"

    private val REASON_CODE = Regex("[a-z0-9._-]{1,64}")

    fun commands(
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "balance",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.BALANCE,
            documentation = "economy/balance",
        ) {
            executesPlayer {
                balanceSelf(
                    playerId,
                    source,
                    integration,
                    settings,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.BALANCE_OTHERS
            ) { player ->
                executes {
                    balanceOther(
                        get(player),
                        source,
                        integration,
                        settings,
                        resolver,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "balancetop",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.BALANCETOP,
            documentation = "economy/balancetop",
        ) {
            executesPlayer {
                ranking(
                    1,
                    source,
                    integration,
                    settings,
                    resolver,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("page", integer(1, 1000)) { page ->
                executesPlayer {
                    ranking(
                        get(page),
                        source,
                        integration,
                        settings,
                        resolver,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "pay",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.PAY,
            documentation = "economy/pay",
        ) {
            argument("player", word()) { player ->
                argument("amount", word()) { amount ->
                    executesPlayer {
                        pay(
                            get(player),
                            get(amount),
                            playerId,
                            source,
                            integration,
                            settings,
                            resolver,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },

        command(
            name = "transactions",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.TRANSACTIONS,
            documentation = "economy/transactions",
        ) {
            executesPlayer {
                history(
                    playerId,
                    1,
                    source,
                    integration,
                    settings,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("page", integer(1, 1000)) { page ->
                executesPlayer {
                    history(
                        playerId,
                        get(page),
                        source,
                        integration,
                        settings,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "eco",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.ECO,
            documentation = "economy/eco",
        ) {
            literal("info") {
                executes {
                    ecoInfo(
                        source,
                        settings,
                        messages,
                        adventure
                    )
                }
            }
            literal("give", permission = CommandPermissions.ECO_GIVE) {
                argument("player", word()) { player ->
                    argument("amount", word()) { amount ->
                        executes {
                            ecoGrant(
                                get(player),
                                get(amount),
                                null,
                                source,
                                integration,
                                settings,
                                resolver,
                                kernel,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                        argument("reason", greedyString()) { reason ->
                            executes {
                                ecoGrant(
                                    get(player),
                                    get(amount),
                                    get(reason),
                                    source,
                                    integration,
                                    settings,
                                    resolver,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation
                                )
                            }
                        }
                    }
                }
            }
            literal("take", permission = CommandPermissions.ECO_TAKE) {
                argument("player", word()) { player ->
                    argument("amount", word()) { amount ->
                        executes {
                            ecoTake(
                                get(player),
                                get(amount),
                                null,
                                source,
                                integration,
                                settings,
                                resolver,
                                kernel,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                        argument("reason", greedyString()) { reason ->
                            executes {
                                ecoTake(
                                    get(player),
                                    get(amount),
                                    get(reason),
                                    source,
                                    integration,
                                    settings,
                                    resolver,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation
                                )
                            }
                        }
                    }
                }
            }
            literal("set", permission = CommandPermissions.ECO_SET) {
                argument("player", word()) { player ->
                    argument("amount", word()) { amount ->
                        executes {
                            ecoSet(
                                get(player),
                                get(amount),
                                null,
                                source,
                                integration,
                                settings,
                                resolver,
                                kernel,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                        argument("reason", greedyString()) { reason ->
                            executes {
                                ecoSet(
                                    get(player),
                                    get(amount),
                                    get(reason),
                                    source,
                                    integration,
                                    settings,
                                    resolver,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation
                                )
                            }
                        }
                    }
                }
            }
            literal("history", permission = CommandPermissions.ECO_HISTORY) {
                argument("player", word()) { player ->
                    executes {
                        ecoHistory(
                            get(player),
                            1,
                            source,
                            integration,
                            settings,
                            resolver,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                    argument("page", integer(1, 1000)) { page ->
                        executes {
                            ecoHistory(
                                get(player),
                                get(page),
                                source,
                                integration,
                                settings,
                                resolver,
                                kernel,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                    }
                }
            }
        },
    )

    // ---- handlers --------------------------------------------------------------------------

    private fun balanceSelf(
        playerId: UUID,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { target ->
            integration.ensureAccount(playerId)
            val notice = when (val result = integration.balance(playerId)) {
                is EconomyBalanceResult.Available -> EconomyMessages.balanceSelf(
                    result.snapshot.amount,
                    display(settings())
                )

                EconomyBalanceResult.Disabled -> EconomyMessages.disabled()
                EconomyBalanceResult.StorageUnavailable -> EconomyMessages.storageUnavailable()
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }

    private fun balanceOther(
        name: String,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        val resolved = resolver.knownByName(name)
            ?: return source.replyErrorLine(
                adventure,
                unknownPlayer(messages, viewerId, name)
            )
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { target ->
            val notice = when (val result = integration.balance(resolved.id)) {
                is EconomyBalanceResult.Available ->
                    EconomyMessages.balanceOther(
                        resolved.name,
                        result.snapshot.amount,
                        display(settings())
                    )

                EconomyBalanceResult.Disabled -> EconomyMessages.disabled()
                EconomyBalanceResult.StorageUnavailable -> EconomyMessages.storageUnavailable()
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                viewerId,
                listOf(notice)
            )
        }
    }

    private fun ranking(
        page: Int,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { target ->
            when (val result = integration.top(page)) {
                is EconomyRankingResult.Available -> {
                    val rows = kernel.onServerThread {
                        result.rows.mapIndexed { index, row ->
                            val rank = (result.page - 1) * result.pageSize + index + 1
                            val name = resolver.onlineById(row.playerId)?.name
                                ?: resolver.knownById(row.playerId)?.name
                                ?: row.playerId.toString()
                            TopRow(rank, name, row.amount)
                        }
                    }
                    kernel.sendPage(
                        target,
                        adventure,
                        presentation,
                        viewerId,
                        EconomyMessages.topPage(
                            rows,
                            result.page,
                            result.pageSize,
                            result.total,
                            display(settings())
                        )
                    )
                }

                EconomyRankingResult.Disabled ->
                    kernel.sendNotices(
                        target,
                        adventure,
                        presentation,
                        viewerId,
                        listOf(EconomyMessages.disabled())
                    )

                EconomyRankingResult.StorageUnavailable ->
                    kernel.sendNotices(
                        target,
                        adventure,
                        presentation,
                        viewerId,
                        listOf(EconomyMessages.storageUnavailable())
                    )
            }
        }
    }

    private fun pay(
        receiverName: String,
        amountText: String,
        payerId: UUID,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val amount = MoneyParsing.parse(amountText)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    payerId,
                    MessageKey("economy.pay.error.invalid_amount")
                )
            )
        val receiver = resolver.knownByName(receiverName)
            ?: return source.replyErrorLine(
                adventure,
                unknownPlayer(
                    messages,
                    payerId,
                    receiverName
                )
            )
        if (receiver.id == payerId) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    payerId,
                    MessageKey("economy.pay.error.same_player")
                )
            )
        }
        if (!settings().allowOfflinePayments
            && resolver.onlineById(receiver.id) == null
        ) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    payerId,
                    MessageKey("economy.pay.error.offline_not_allowed"),
                    receiverName
                )
            )
        }
        val payerName = resolver.onlineById(payerId)?.name ?: payerId.toString()
        val receiverTarget = resolver.onlineById(receiver.id)
                ?.let { CommandFeedbackTarget.Player(it.id) }
        return economyLaunch(source, kernel, messages, adventure) { target ->
            val result = integration.transfer(
                EconomyTransferRequest(
                    actor = EconomyActorDescriptor(
                        EconomyActorKind.PLAYER,
                        payerId.toString()
                    ),
                    payerId = payerId,
                    receiverId = receiver.id,
                    amount = amount.decimal,
                    originId = ORIGIN,
                    requestKey = commandKey(),
                    reasonCode = "player.pay",
                )
            )
            when (result) {
                is EconomyTransactionResult.Committed,
                is EconomyTransactionResult.AlreadyCommitted,
                    -> {
                    val senderBalance = balanceOf(integration, payerId)
                    kernel.sendNotices(
                        target,
                        adventure,
                        presentation,
                        payerId,
                        listOf(
                            EconomyMessages.paySuccessSender(
                                receiver.name,
                                amount.decimal,
                                senderBalance,
                                display(settings())
                            )
                        )
                    )
                    receiverTarget?.let { destination ->
                        val receiverBalance = balanceOf(
                            integration,
                            receiver.id
                        )
                        kernel.sendNotices(
                            destination,
                            adventure,
                            presentation,
                            receiver.id,
                            listOf(
                                EconomyMessages.paySuccessReceiver(
                                    payerName,
                                    amount.decimal,
                                    receiverBalance,
                                    display(settings())
                                )
                            )
                        )
                    }
                }

                else -> kernel.sendNotices(
                    target,
                    adventure,
                    presentation,
                    payerId,
                    listOf(EconomyMessages.transactionFailure(result))
                )
            }
        }
    }

    private fun history(
        subject: UUID,
        page: Int,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { destination ->
            renderHistory(
                destination,
                viewerId,
                subject,
                page,
                integration,
                settings,
                kernel,
                adventure,
                presentation
            )
        }
    }

    private fun ecoHistory(
        name: String,
        page: Int,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        val resolved = resolver.knownByName(name)
            ?: return source.replyErrorLine(
                adventure,
                unknownPlayer(messages, viewerId, name)
            )
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { destination ->
            renderHistory(
                destination,
                viewerId,
                resolved.id,
                page,
                integration,
                settings,
                kernel,
                adventure,
                presentation
            )
        }
    }

    private suspend fun renderHistory(
        destination: CommandFeedbackTarget,
        viewerId: UUID?,
        subject: UUID,
        page: Int,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        kernel: RuntimeKernel,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ) {
        when (val result = integration.history(subject, page)) {
            is EconomyHistoryResult.Available -> {
                val rows = result.rows.map { row ->
                    HistoryRow(
                        row.kind.name,
                        row.delta,
                        row.balanceAfter
                    )
                }
                kernel.sendPage(
                    destination,
                    adventure,
                    presentation,
                    viewerId,
                    EconomyMessages.historyPage(
                        rows,
                        result.page,
                        result.pageSize,
                        result.total,
                        display(settings())
                    )
                )
            }

            EconomyHistoryResult.Disabled ->
                kernel.sendNotices(
                    destination,
                    adventure,
                    presentation,
                    viewerId,
                    listOf(EconomyMessages.disabled())
                )

            EconomyHistoryResult.StorageUnavailable ->
                kernel.sendNotices(
                    destination,
                    adventure,
                    presentation,
                    viewerId,
                    listOf(EconomyMessages.storageUnavailable())
                )
        }
    }

    private fun ecoInfo(
        source: CommandSourceStack,
        settings: () -> EconomySettings,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): Int {
        val economy = settings()
        return source.replyLine(
            adventure,
            messages.renderFor(
                source.player?.uuid,
                MessageKey("economy.eco.info"),
                economy.currencyName,
                economy.maximumBalance.toPlainString()
            ),
            DisplayChannel.CHAT,
        )
    }

    private fun ecoGrant(
        name: String,
        amountText: String,
        reason: String?,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        val amount = MoneyParsing.parse(amountText)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    viewerId,
                    MessageKey("economy.pay.error.invalid_amount")
                )
            )
        val target = resolver.knownByName(name)
            ?: return source.replyErrorLine(
                adventure,
                unknownPlayer(messages, viewerId, name)
            )
        val actor = adminActor(source)
        val reasonCode = normalizeReason(reason)
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { destination ->
            val result = integration.grant(
                EconomyGrantRequest(
                    actor,
                    target.id,
                    amount.decimal,
                    ORIGIN,
                    commandKey(),
                    reasonCode
                )
            )
            val notices = listOf(
                when (result) {
                    is EconomyTransactionResult.Committed,
                    is EconomyTransactionResult.AlreadyCommitted ->
                        EconomyMessages.ecoGrant(
                            target.name,
                            amount.decimal,
                            display(settings())
                        )

                    else -> EconomyMessages.transactionFailure(result)
                }
            )
            kernel.sendNotices(
                destination,
                adventure,
                presentation,
                viewerId,
                notices
            )
        }
    } // placeholder replaced below

    private fun ecoTake(
        name: String,
        amountText: String,
        reason: String?,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        val amount = MoneyParsing.parse(amountText)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    viewerId,
                    MessageKey("economy.pay.error.invalid_amount")
                )
            )
        val target = resolver.knownByName(name)
            ?: return source.replyErrorLine(
                adventure,
                unknownPlayer(messages, viewerId, name)
            )
        val actor = adminActor(source)
        val reasonCode = normalizeReason(reason)
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { destination ->
            val result = integration.take(
                EconomyTakeRequest(
                    actor,
                    target.id,
                    amount.decimal,
                    ORIGIN,
                    commandKey(),
                    reasonCode
                )
            )
            val notices = listOf(
                when (result) {
                    is EconomyTransactionResult.Committed,
                    is EconomyTransactionResult.AlreadyCommitted ->
                        EconomyMessages.ecoTake(
                            target.name,
                            amount.decimal,
                            display(settings())
                        )

                    else -> EconomyMessages.transactionFailure(result)
                }
            )
            kernel.sendNotices(
                destination,
                adventure,
                presentation,
                viewerId,
                notices
            )
        }
    }

    private fun ecoSet(
        name: String,
        amountText: String,
        reason: String?,
        source: CommandSourceStack,
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        val amount = MoneyParsing.parse(amountText)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    viewerId,
                    MessageKey("economy.pay.error.invalid_amount")
                )
            )
        val target = resolver.knownByName(name)
            ?: return source.replyErrorLine(
                adventure,
                unknownPlayer(messages, viewerId, name)
            )
        val actor = adminActor(source)
        val reasonCode = normalizeReason(reason)
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { destination ->
            val result = integration.set(
                EconomySetRequest(
                    actor,
                    target.id,
                    amount.decimal,
                    ORIGIN,
                    commandKey(),
                    reasonCode
                )
            )
            val notices = listOf(
                when (result) {
                    is EconomyTransactionResult.Committed,
                    is EconomyTransactionResult.AlreadyCommitted ->
                        EconomyMessages.ecoSet(
                            target.name,
                            amount.decimal,
                            display(settings())
                        )

                    is EconomyTransactionResult.NoChange ->
                        EconomyMessages.ecoSetNoChange(
                            target.name,
                            amount.decimal,
                            display(settings())
                        )

                    else -> EconomyMessages.transactionFailure(result)
                }
            )
            kernel.sendNotices(
                destination,
                adventure,
                presentation,
                viewerId,
                notices
            )
        }
    }

    private fun adminActor(source: CommandSourceStack): EconomyActorDescriptor {
        return when (val player = source.player) {
            null -> EconomyActorDescriptor(
                EconomyActorKind.ADMIN,
                "console"
            )

            else -> EconomyActorDescriptor(
                EconomyActorKind.ADMIN,
                player.uuid.toString()
            )
        }
    }

    private suspend fun balanceOf(
        integration: EconomyIntegrationPort,
        playerId: UUID
    ): BigDecimal =
        when (val result = integration.balance(playerId)) {
            is EconomyBalanceResult.Available -> result.snapshot.amount
            else -> BigDecimal.ZERO
        }

    private fun normalizeReason(reason: String?): String {
        val candidate = reason?.trim()?.lowercase()
        return if (candidate != null && REASON_CODE.matches(candidate)) {
            candidate
        } else {
            "manual.admin"
        }
    }

    private fun commandKey(): String = "cmd:${UUID.randomUUID()}"

    private fun unknownPlayer(
        messages: LocalizedMessages,
        viewerId: UUID?,
        name: String
    ): Component =
        messages.renderFor(
            viewerId,
            MessageKey("economy.pay.error.unknown_player"),
            name
        )

    private fun display(settings: EconomySettings): EconomyDisplay =
        EconomyDisplay(settings.currencyName)

}

/**
 * Shared async economy orchestration: acknowledge immediately, then run the block with the captured
 * feedback target. Never holds a [CommandSourceStack] across suspension.
 */
internal fun economyLaunch(
    source: CommandSourceStack,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    block: suspend (CommandFeedbackTarget) -> Unit,
): Int {
    val playerId = source.player?.uuid
    val target = source.feedbackTarget()
    return when (kernel.launch { block(target) }) {
        null -> source.replyErrorLine(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.error.runtime_stopping")
            )
        )

        else -> source.replyLine(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("economy.pending")
            ),
            DisplayChannel.ACTION_BAR
        )
    }
}
