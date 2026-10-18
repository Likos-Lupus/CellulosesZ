package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.item
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.format.EconomyDisplay
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.integration.*
import top.likoslupus.cellulosesz.economy.ledger.EconomyDataException
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.money.MoneyParsing
import top.likoslupus.cellulosesz.economy.sale.*
import top.likoslupus.cellulosesz.economy.worth.WorthService
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * The collaborators shared by the worth and sale commands. Commands read prices from the in-memory
 * cache on the server thread; only the ledger and the intent repository ever suspend.
 */
internal class WorthSellContext(
    val integration: EconomyIntegrationPort,
    val settings: () -> EconomySettings,
    val kernel: RuntimeKernel,
    val messages: LocalizedMessages,
    val adventure: AdventureRuntime,
    val presentation: TextPresentation,
    val permissions: PermissionService,
    val worth: WorthService,
    val intents: SaleIntentRepository,
    val inventory: SaleInventory,
    val codec: SaleItemCodec,
    val recovery: SaleRecoveryService,
    val now: () -> Instant,
)

/**
 * `/worth`, `/sell` and `/setworth`. The worth lookup is synchronous against the hydrated cache; a
 * sale captures the exact stacks on the server thread, then hands the removal, the durable intent
 * and the single ledger credit to an asynchronous task. No handler ever writes a balance directly.
 */
internal object WorthSellCommands {

    fun commands(context: WorthSellContext): List<CommandDefinition> = listOf(
        command(
            name = "worth",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.WORTH,
            documentation = "economy/worth",
        ) {
            executesPlayer {
                worthHeld(
                    context,
                    source,
                    playerId,
                    null
                )
            }
            literal("hand") {
                executesPlayer {
                    worthHeld(
                        context,
                        source,
                        playerId,
                        null
                    )
                }
                argument("amount", word()) { amount ->
                    executesPlayer {
                        worthHeld(
                            context,
                            source,
                            playerId,
                            get(amount)
                        )
                    }
                }
            }
            literal("inventory") {
                executesPlayer { worthInventory(context, source, playerId) }
            }
            literal("blocks") {
                executesPlayer {
                    worthBlocks(
                        context,
                        source,
                        playerId,
                        null
                    )
                }
                argument("amount", word()) { amount ->
                    executesPlayer {
                        worthBlocks(
                            context,
                            source,
                            playerId,
                            get(amount)
                        )
                    }
                }
            }
            argument("item", item()) { itemKey ->
                executesPlayer {
                    worthItem(
                        context,
                        source,
                        playerId,
                        itemIdOf(get(itemKey)),
                        null
                    )
                }
                argument("amount", word()) { amount ->
                    executesPlayer {
                        worthItem(
                            context,
                            source,
                            playerId,
                            itemIdOf(get(itemKey)),
                            get(amount)
                        )
                    }
                }
            }
        },

        command(
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
        },

        command(
            name = "setworth",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.SET_WORTH,
            documentation = "economy/setworth",
        ) {
            argument("item", item()) { itemKey ->
                argument("price", word()) { price ->
                    executes {
                        setWorth(
                            context,
                            source,
                            itemIdOf(get(itemKey)),
                            get(price)
                        )
                    }
                }
            }
            argument("price", word()) { price ->
                executesPlayer {
                    setWorth(
                        context,
                        source,
                        itemIdOf(source.player),
                        get(price)
                    )
                }
            }
        },
    )

    // ---- /worth ----------------------------------------------------------------------------

    private fun worthHeld(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        amountText: String?,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val amount = when (amountText) {
            null -> null
            else -> CountParsing.signed(amountText)
                ?: return invalidAmount(context, source, playerId)
        }
        val slot = context.inventory.scan(
            playerId,
            SaleScope.HAND,
            null
        ).firstOrNull()
            ?: return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthHeldEmpty(),
            )
        val price = context.worth.unitPrice(slot.itemId)
            ?: return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthMissing(slot.itemId),
            )
        return replyWorthUnit(
            context,
            source,
            playerId,
            slot.itemId,
            price,
            amount ?: slot.count
        )
    }

    private fun worthItem(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        itemId: String?,
        amountText: String?,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val amount = when (amountText) {
            null -> null
            else -> CountParsing.signed(amountText)
                ?: return invalidAmount(context, source, playerId)
        }
        val resolved = itemId
            ?: return source.replyError(
                context.adventure,
                context.messages.renderFor(playerId, MessageKey("economy.worth.unknown_item")),
            )
        val owned = context.inventory.scan(
            playerId,
            SaleScope.ITEM,
            resolved
        ).sumOf { it.count }
        val price = context.worth.unitPrice(resolved)
            ?: return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthMissing(resolved),
            )
        return replyWorthUnit(
            context,
            source,
            playerId,
            resolved,
            price,
            amount ?: owned
        )
    }

    private fun worthInventory(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val slots = context.inventory.scan(
            playerId,
            SaleScope.INVENTORY,
            null
        )
        var total = BigDecimal.ZERO
        var items = 0
        slots.forEach { slot ->
            val price = context.worth.unitPrice(slot.itemId)
                ?: return@forEach
            total = total.add(price.decimal.multiply(BigDecimal(slot.count)))
            items += slot.count
        }
        return when {
            items == 0 -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthNothing(),
            )

            else -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthInventory(
                    total,
                    items,
                    context.display()
                ),
            )
        }
    }

    private fun worthBlocks(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        amountText: String?,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val cap = when (amountText) {
            null -> null
            else -> CountParsing.signed(amountText)
                    ?.takeIf { it > 0 }
                ?: return invalidAmount(context, source, playerId)
        }
        val slots = context.inventory.scan(
            playerId,
            SaleScope.BLOCKS,
            null
        )
        var total = BigDecimal.ZERO
        var items = 0
        slots.forEach { slot ->
            if (cap != null && items >= cap) {
                return@forEach
            }
            val price = context.worth.unitPrice(slot.itemId)
                ?: return@forEach
            val count = when (cap) {
                null -> slot.count
                else -> minOf(slot.count, cap - items)
            }
            total = total.add(price.decimal.multiply(BigDecimal(count)))
            items += count
        }
        return when {
            items == 0 -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthNothing(),
            )

            else -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthBlocks(
                    total,
                    items,
                    context.display()
                ),
            )
        }
    }

    private fun replyWorthUnit(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        itemId: String,
        price: Money,
        count: Int,
    ): Int {
        val total = price.decimal.multiply(BigDecimal(count))
        return source.replyNotices(
            context.adventure,
            context.presentation,
            playerId,
            listOf(
                EconomyMessages.worthUnit(
                    itemId,
                    price.decimal,
                    context.display()
                ),
                EconomyMessages.worthTotal(
                    itemId,
                    count,
                    total,
                    context.display()
                ),
            ),
        )
    }

    // ---- /sell -----------------------------------------------------------------------------

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

    // ---- /setworth -------------------------------------------------------------------------

    private fun setWorth(
        context: WorthSellContext,
        source: CommandSourceStack,
        itemId: String?,
        priceText: String,
    ): Int {
        val playerId = source.player?.uuid
        val resolved = itemId
            ?: return source.replyError(
                context.adventure,
                context.messages.renderFor(
                    playerId,
                    MessageKey("economy.worth.held.empty")
                ),
            )
        val price = MoneyParsing.parse(priceText)
            ?: return invalidAmount(context, source, playerId)
        val actor = source.player?.gameProfile?.name ?: CONSOLE_ACTOR

        return economyLaunch(
            source,
            context.kernel,
            context.messages,
            context.adventure
        ) { target ->
            val saved = context.worth.set(resolved, price, actor)
            val notice = when {
                saved -> EconomyMessages.setWorth(
                    resolved,
                    price.decimal,
                    context.display()
                )

                else -> EconomyMessages.setWorthFailed()
            }
            context.deliver(target, playerId, notice)
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    private fun itemIdOf(input: ItemInput?): String? {
        val resolved = input ?: return null
        return resolved.item().unwrapKey()
                .map { it.identifier().toString() }
                .orElse(null)
    }

    private fun itemIdOf(player: ServerPlayer?): String? =
        player?.mainHandItem?.takeIf { !it.isEmpty }?.let {
            BuiltInRegistries.ITEM.getKey(it.item).toString()
        }


    private fun invalidAmount(
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

    private const val CONSOLE_ACTOR = "console"

    private const val MULTIPLE_ITEMS_LABEL = "cellulosesz:multiple"

    private fun WorthSellContext.display(): EconomyDisplay =
        EconomyDisplay(settings().currencyName)

    private fun WorthSellContext.disabled(source: CommandSourceStack, playerId: UUID?): Int =
        source.replyNotice(
            adventure,
            presentation,
            playerId,
            EconomyMessages.disabled()
        )

    private suspend fun WorthSellContext.deliver(
        target: CommandFeedbackTarget,
        playerId: UUID?,
        notice: Notice,
    ) {
        kernel.sendNotices(
            target,
            adventure,
            presentation,
            playerId,
            listOf(notice)
        )
    }

    private suspend fun WorthSellContext.balanceOf(playerId: UUID): BigDecimal =
        when (val result = integration.balance(playerId)) {
            is EconomyBalanceResult.Available -> result.snapshot.amount
            else -> BigDecimal.ZERO
        }

}
