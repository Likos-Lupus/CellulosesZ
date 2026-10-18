package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.format.HistoryRow
import top.likoslupus.cellulosesz.economy.integration.EconomyHistoryResult
import top.likoslupus.cellulosesz.economy.integration.EconomyIntegrationPort
import java.util.*

/**
 * `/transactions` — a player's own ledger history. The read-only `/eco info` and `/eco history`
 * branches belong to the same responsibility but to the `/eco` root (owned by
 * [EconomyAdminCommands]), so they are declared as [CommandSyntax] branches and reused there.
 */
internal object EconomyHistoryCommands {

    fun commands(
        integration: EconomyIntegrationPort,
        settings: () -> EconomySettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> =
        listOf(
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
        )

}

/** `/eco info`: the currency name and the balance ceiling. */
internal fun CommandSyntax.ecoInfoNode(
    settings: () -> EconomySettings,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
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
}

/** `/eco history <player> [page]`: another player's ledger history. */
internal fun CommandSyntax.ecoHistoryNodes(
    integration: EconomyIntegrationPort,
    settings: () -> EconomySettings,
    resolver: KnownPlayerResolver,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
) {
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
