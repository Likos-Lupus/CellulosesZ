package top.likoslupus.cellulosesz.economy.command

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.integration.EconomyIntegrationPort
import top.likoslupus.cellulosesz.economy.sale.SaleIntentRepository
import top.likoslupus.cellulosesz.economy.sale.SaleInventory
import top.likoslupus.cellulosesz.economy.sale.SaleItemCodec
import top.likoslupus.cellulosesz.economy.sale.SaleRecoveryService
import top.likoslupus.cellulosesz.economy.worth.WorthService
import java.time.Instant

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
 * `/worth`, `/sell` and `/setworth`. The declarations live next to their handlers, one object per
 * responsibility ([WorthCommands], [SellCommands], [SetWorthCommands]); this entry point only
 * assembles them in registration order. Shared helpers are in `WorthSellSupport.kt`.
 */
internal object WorthSellCommands {

    fun commands(context: WorthSellContext): List<CommandDefinition> = listOf(
        WorthCommands.definition(context),
        SellCommands.definition(context),
        SetWorthCommands.definition(context),
    )

}
