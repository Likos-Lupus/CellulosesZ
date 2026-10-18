package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.format.TopRow
import top.likoslupus.cellulosesz.economy.integration.EconomyBalanceResult
import top.likoslupus.cellulosesz.economy.integration.EconomyIntegrationPort
import top.likoslupus.cellulosesz.economy.integration.EconomyRankingResult
import java.util.UUID

/**
 * `/balance` (own or another player's) and `/balancetop`. Both read the balance projection through
 * the [EconomyIntegrationPort] and only ever render a notice or a page.
 */
internal object BalanceCommands {

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
    )

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

}
