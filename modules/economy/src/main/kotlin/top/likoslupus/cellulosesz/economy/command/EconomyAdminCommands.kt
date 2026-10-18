package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyErrorLine
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.integration.*
import top.likoslupus.cellulosesz.economy.money.MoneyParsing

/**
 * The `/eco` administration root: `give`, `take` and `set` plus the read-only `info` and `history`
 * branches declared by [ecoInfoNode] and [ecoHistoryNodes]. Every mutation is one balanced ledger
 * transaction against the [EconomyIntegrationPort].
 */
internal object EconomyAdminCommands {

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
            name = "eco",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.ECO,
            documentation = "economy/eco",
        ) {
            ecoInfoNode(
                settings,
                messages,
                adventure
            )
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
            ecoHistoryNodes(
                integration,
                settings,
                resolver,
                kernel,
                messages,
                adventure,
                presentation
            )
        },
    )

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
                    COMMAND_ORIGIN,
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
    }

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
                    COMMAND_ORIGIN,
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
                    COMMAND_ORIGIN,
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

    private fun normalizeReason(reason: String?): String {
        val candidate = reason?.trim()?.lowercase()
        return if (candidate != null && REASON_CODE.matches(candidate)) {
            candidate
        } else {
            "manual.admin"
        }
    }

}
