package top.likoslupus.cellulosesz.utility.item

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

internal object ItemUtilityCommands {

    fun commands(
        service: ItemUtilityService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        audit: AdministrativeActionAuditPort,
        kernel: RuntimeKernel,
        preferences: PlayerPreferenceService,
    ): List<CommandDefinition> =
        ItemAdjustmentCommands.commands(
            service,
            known,
            messages,
            adventure,
            presentation,
        ) + ClearInventoryCommands.commands(
            service,
            known,
            messages,
            adventure,
            presentation,
            audit,
            kernel,
            preferences,
        )

}
