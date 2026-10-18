package top.likoslupus.cellulosesz.economy.command

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.integration.EconomyIntegrationPort
import top.likoslupus.cellulosesz.economy.payment.PayConfirmationState
import top.likoslupus.cellulosesz.economy.payment.PaymentEligibility

/**
 * Every economy command. Each resolves its target on the server thread, builds an authorized
 * immutable request, and hands it to the single [EconomyIntegrationPort]; none writes a balance
 * directly and none blocks the server thread on JDBC.
 *
 * The declarations themselves live next to their handlers, one object per responsibility
 * ([BalanceCommands], [PayCommands], [EconomyHistoryCommands], [EconomyAdminCommands]); this entry
 * point only assembles them in registration order. Shared helpers are in `EconomyCommandSupport.kt`.
 */
internal object EconomyCommands {

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
    ): List<CommandDefinition> = buildList {
        addAll(
            BalanceCommands.commands(
                integration,
                settings,
                resolver,
                kernel,
                messages,
                adventure,
                presentation,
            )
        )
        addAll(
            PayCommands.commands(
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
        )
        addAll(
            EconomyHistoryCommands.commands(
                integration,
                settings,
                kernel,
                messages,
                adventure,
                presentation,
            )
        )
        addAll(
            EconomyAdminCommands.commands(
                integration,
                settings,
                resolver,
                kernel,
                messages,
                adventure,
                presentation,
            )
        )
    }

}
