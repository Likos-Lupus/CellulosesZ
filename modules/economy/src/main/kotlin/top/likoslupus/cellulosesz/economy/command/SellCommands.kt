package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.item
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.presentation.DisplayChannel
import top.likoslupus.cellulosesz.core.text.presentation.replyErrorLine
import top.likoslupus.cellulosesz.core.text.presentation.replyLine
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.integration.*
import top.likoslupus.cellulosesz.economy.ledger.EconomyDataException
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.sale.*
import java.math.BigDecimal
import java.util.*

/** `/sell` for the held item, the whole inventory, blocks or a named item. */
internal object SellCommands {

    fun definition(context: WorthSellContext): CommandDefinition = command(
        name = "sell",
        category = CommandCategory.ECONOMY,
        permission = CommandPermissions.SELL,
        documentation = "economy/sell",
    ) {
        literal("hand") {
            executesPlayer {
                sell(
                    context,
                    SaleScope.HAND,
                    null,
                    null,
                    source,
                    playerId
                )
            }
            argument("amount", word()) { amount ->
                executesPlayer {
                    sell(
                        context,
                        SaleScope.HAND,
                        null,
                        get(amount),
                        source,
                        playerId
                    )
                }
            }
        }
        literal("inventory") {
            executesPlayer {
                sell(
                    context,
                    SaleScope.INVENTORY,
                    null,
                    null,
                    source,
                    playerId
                )
            }
        }
        literal("blocks") {
            executesPlayer {
                sell(
                    context,
                    SaleScope.BLOCKS,
                    null,
                    null,
                    source,
                    playerId
                )
            }
            argument("amount", word()) { amount ->
                executesPlayer {
                    sell(
                        context,
                        SaleScope.BLOCKS,
                        null,
                        get(amount),
                        source,
                        playerId
                    )
                }
            }
        }
        argument("item", item()) { itemKey ->
            executesPlayer {
                sell(
                    context,
                    SaleScope.ITEM,
                    itemIdOf(get(itemKey)),
                    null,
                    source,
                    playerId
                )
            }
            argument("amount", word()) { amount ->
                executesPlayer {
                    sell(
                        context,
                        SaleScope.ITEM,
                        itemIdOf(get(itemKey)),
                        get(amount),
                        source,
                        playerId
                    )
                }
            }
        }
    }

    private fun sell(
        context: WorthSellContext,
        scope: SaleScope,
        itemId: String?,
        amountText: String?,
        source: CommandSourceStack,
        playerId: UUID,
    ): Int {
        val settings = context.settings()
        if (!settings.enabled) {
            return context.disabled(source, playerId)
        }
        if ((scope == SaleScope.INVENTORY || scope == SaleScope.BLOCKS)
            && !settings.sell.allowBulk
            && !source.requiresPermission(
                context.permissions,
                CommandPermissions.SELL_BULK
            )
        ) {
            return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.sellBulkDenied(),
            )
        }

        val limit = when (amountText) {
            null -> null
            else -> CountParsing.positive(amountText)
                ?: return invalidAmount(context, source, playerId)
        }
        val player = source.player ?: return 0
        val playerName = player.gameProfile.name

        val slots = context.inventory.scan(playerId, scope, itemId)
        if (scope == SaleScope.HAND && slots.isEmpty()) {
            return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.sellHeldEmpty(),
            )
        }

        val namedPolicy = when {
            settings.sell.allowNamedItems -> SkipPolicy.SKIP
            else -> SkipPolicy.REFUSE
        }
        val unpricedPolicy = when (scope) {
            SaleScope.HAND,
            SaleScope.ITEM -> SkipPolicy.REFUSE

            SaleScope.INVENTORY,
            SaleScope.BLOCKS -> SkipPolicy.SKIP
        }
        val multiplier = SellMultipliers.resolve(settings.sell.multipliers) { node ->
            source.requiresPermission(
                context.permissions,
                PermissionSpec(
                    node,
                    VanillaPermissionFallback.DENY_ALL,
                    "Sell price multiplier"
                ),
            )
        }
        val maximumMinor = Money.of(settings.maximumBalance).toMinorUnitsExact()
        val candidates = slots.map { slot ->
            SellCandidate(
                slot = slot.slot,
                itemId = slot.itemId,
                count = slot.count,
                unitPriceMinor = context.worth.unitPrice(slot.itemId)?.toMinorUnitsExact(),
                customNamed = slot.customNamed,
            )
        }

        val planned = when (
            val result = SellPlanner.plan(
                candidates = candidates,
                namedPolicy = namedPolicy,
                unpricedPolicy = unpricedPolicy,
                multiplier = multiplier,
                maximumMinor = maximumMinor,
                limit = limit,
            )
        ) {
            is SellPlanResult.Planned -> result

            SellPlanResult.Nothing,
            SellPlanResult.ZeroValue ->
                return source.replyNotice(
                    context.adventure,
                    context.presentation,
                    playerId,
                    EconomyMessages.sellNothing(),
                )

            is SellPlanResult.Unpriced ->
                return source.replyNotice(
                    context.adventure,
                    context.presentation,
                    playerId,
                    EconomyMessages.sellUnpriced(result.itemId),
                )

            is SellPlanResult.NamedNotAllowed ->
                return source.replyNotice(
                    context.adventure,
                    context.presentation,
                    playerId,
                    EconomyMessages.sellNamed(result.itemId),
                )

            SellPlanResult.ExceedsCap ->
                return source.replyNotice(
                    context.adventure,
                    context.presentation,
                    playerId,
                    EconomyMessages.sellExceeds(),
                )
        }

        val snapshot = context.inventory.snapshot(playerId, planned.lines)
        if (snapshot.size != planned.lines.size) {
            return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.sellChanged(),
            )
        }
        val payload = context.codec.encode(snapshot)
        val itemLabel = planned.lines
                .map { it.itemId }
                .distinct()
                .singleOrNull()
            ?: MULTIPLE_ITEMS_LABEL

        val target = source.feedbackTarget()
        val job = context.kernel.launch {
            runSale(
                context = context,
                target = target,
                playerId = playerId,
                playerName = playerName,
                planned = planned,
                payload = payload,
                itemLabel = itemLabel,
            )
        }
        return when (job) {
            null -> source.replyErrorLine(
                context.adventure,
                context.messages.renderFor(
                    playerId,
                    MessageKey("core.error.runtime_stopping")
                ),
            )

            else -> pendingAcknowledgement(context, source, playerId)
        }
    }

    private suspend fun runSale(
        context: WorthSellContext,
        target: CommandFeedbackTarget,
        playerId: UUID,
        playerName: String,
        planned: SellPlanResult.Planned,
        payload: String,
        itemLabel: String,
    ) {
        val maximum = Money.of(context.settings().maximumBalance)
        val balance = when (val result = context.integration.balance(playerId)) {
            is EconomyBalanceResult.Available ->
                result.snapshot.amount

            EconomyBalanceResult.Disabled ->
                return context.deliver(
                    target,
                    playerId,
                    EconomyMessages.disabled(),
                )

            EconomyBalanceResult.StorageUnavailable ->
                return context.deliver(
                    target,
                    playerId,
                    EconomyMessages.storageUnavailable(),
                )
        }
        val total = Money.fromMinorUnits(planned.totalMinor)
        if (balance.add(total.decimal).compareTo(maximum.decimal) > 0) {
            return context.deliver(
                target,
                playerId,
                EconomyMessages.sellExceeds()
            )
        }

        val saleId = UUID.randomUUID()
        val at = context.now()
        val record = SaleIntentRecord(
            saleId = saleId,
            playerId = playerId,
            playerName = playerName,
            state = SaleIntentState.PREPARED,
            itemId = itemLabel,
            itemCount = planned.itemCount,
            totalMinor = planned.totalMinor,
            payload = payload,
            transactionId = null,
            createdAtMs = at.toEpochMilli(),
            updatedAtMs = at.toEpochMilli(),
        )
        try {
            context.intents.insert(record)
        } catch (_: EconomyDataException) {
            // Nothing has left the inventory yet, so the sale simply did not happen.
            return context.deliver(
                target,
                playerId,
                EconomyMessages.sellFailed()
            )
        }

        val removed = context.kernel.onServerThread {
            context.inventory.remove(playerId, planned.lines)
        }
        if (removed == null) {
            runCatching { context.intents.markRolledBack(saleId, context.now()) }
            return context.deliver(
                target,
                playerId,
                EconomyMessages.sellChanged()
            )
        }
        runCatching { context.intents.markTaken(saleId, context.now()) }

        val credited = context.integration.grant(
            EconomyGrantRequest(
                actor = EconomyActorDescriptor(
                    EconomyActorKind.PLAYER,
                    playerId.toString()
                ),
                playerId = playerId,
                amount = total.decimal,
                originId = SELL_ORIGIN,
                requestKey = saleId.toString(),
                reasonCode = SELL_REASON,
                referenceId = itemLabel,
            )
        )
        when (credited) {
            is EconomyTransactionResult.Committed,
            is EconomyTransactionResult.AlreadyCommitted -> {
                val transactionId = when (credited) {
                    is EconomyTransactionResult.Committed -> credited.receipt.transactionId
                    is EconomyTransactionResult.AlreadyCommitted -> credited.receipt.transactionId
                }
                runCatching {
                    context.intents.markCommitted(
                        saleId,
                        transactionId,
                        context.now()
                    )
                }
                val balanceAfter = context.balanceOf(playerId)
                context.deliver(
                    target,
                    playerId,
                    EconomyMessages.sellSuccess(
                        itemLabel,
                        planned.itemCount,
                        total.decimal,
                        balanceAfter,
                        context.display(),
                    ),
                )
            }

            else -> {
                // The credit failed after removal: hand the exact stacks back (or park them until
                // the owner is next online). The ledger was never written, so money is unchanged.
                context.recovery.restoreOrPark(record.copy(state = SaleIntentState.TAKEN))
                context.deliver(target, playerId, EconomyMessages.sellFailed())
            }
        }
    }

    private fun pendingAcknowledgement(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
    ): Int =
        source.replyLine(
            context.adventure,
            context.messages.renderFor(
                playerId,
                MessageKey("economy.pending")
            ),
            DisplayChannel.ACTION_BAR,
        )

    private suspend fun WorthSellContext.balanceOf(playerId: UUID): BigDecimal =
        when (val result = integration.balance(playerId)) {
            is EconomyBalanceResult.Available -> result.snapshot.amount
            else -> BigDecimal.ZERO
        }

    private const val MULTIPLE_ITEMS_LABEL = "cellulosesz:multiple"

}

internal fun invalidAmount(
    context: WorthSellContext,
    source: CommandSourceStack,
    playerId: UUID?,
): Int =
    source.replyError(
        context.adventure,
        context.messages.renderFor(
            playerId,
            MessageKey("economy.pay.error.invalid_amount")
        ),
    )
