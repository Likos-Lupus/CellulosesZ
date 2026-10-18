package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyErrorLine
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.integration.*
import top.likoslupus.cellulosesz.economy.money.MoneyParsing
import top.likoslupus.cellulosesz.economy.payment.*
import java.math.BigDecimal
import java.util.*

/**
 * `/pay` and its handler. The two payment preferences (`/paytoggle` and `/payconfirmtoggle`) live in
 * [PayToggleCommands]; this entry point still assembles both so [EconomyCommands] keeps one
 * `PayCommands.commands(...)` call with an unchanged signature.
 */
internal object PayCommands {

    fun commands(
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
        eligibility: PaymentEligibility,
        confirmations: PayConfirmationState,
    ): List<CommandDefinition> = listOf(
        payDefinition(
            integration,
            settings,
            resolver,
            kernel,
            messages,
            adventure,
            presentation,
            permissions,
            preferences,
            eligibility,
            confirmations,
        ),
    ) + PayToggleCommands.commands(
        preferences,
        confirmations,
        kernel,
        messages,
        adventure,
        presentation,
    )

    private fun payDefinition(
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        resolver: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
        eligibility: PaymentEligibility,
        confirmations: PayConfirmationState,
    ): CommandDefinition =
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
                            presentation,
                            permissions,
                            preferences,
                            eligibility,
                            confirmations,
                        )
                    }
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
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
        eligibility: PaymentEligibility,
        confirmations: PayConfirmationState,
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
        when (
            PayEligibilityGate.decide(
                receiverAcceptsPay = preferences.isEnabled(
                    receiver.id,
                    PlayerPreference.ACCEPTING_PAY,
                ),
                payerBypasses = source.requiresPermission(
                    permissions,
                    CommandPermissions.PAY_TOGGLE_BYPASS,
                ),
                receiverExcluded = settings().pay.excludesIgnoreList
                        && !eligibility.allows(payerId, receiver.id),
            )
        ) {
            PayEligibilityDecision.ReceiverDisabled -> return source.replyNotice(
                adventure,
                presentation,
                payerId,
                EconomyMessages.payReceiverDisabled(receiver.name),
            )

            PayEligibilityDecision.ReceiverBlocked -> return source.replyNotice(
                adventure,
                presentation,
                payerId,
                EconomyMessages.payBlocked(receiver.name),
            )

            PayEligibilityDecision.Allowed -> Unit
        }
        if (preferences.isEnabled(payerId, PlayerPreference.PAY_CONFIRMATION)) {
            when (
                val confirmation = confirmations.request(
                    payerId,
                    receiver.id,
                    amount,
                    receiver.name,
                )
            ) {
                PayConfirmationDecision.Execute -> Unit
                is PayConfirmationDecision.Confirm -> return source.replyNotice(
                    adventure,
                    presentation,
                    payerId,
                    EconomyMessages.payConfirm(
                        confirmation.receiverName,
                        confirmation.amount.decimal,
                        display(settings()),
                    ),
                )
            }
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
                    originId = COMMAND_ORIGIN,
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

    private suspend fun balanceOf(
        integration: EconomyIntegrationPort,
        playerId: UUID
    ): BigDecimal =
        when (val result = integration.balance(playerId)) {
            is EconomyBalanceResult.Available -> result.snapshot.amount
            else -> BigDecimal.ZERO
        }

}
