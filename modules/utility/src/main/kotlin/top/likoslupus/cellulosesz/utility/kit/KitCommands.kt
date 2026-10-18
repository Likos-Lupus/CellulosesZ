package top.likoslupus.cellulosesz.utility.kit

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

internal object KitCommands {

    fun commands(
        service: KitService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> =
        KitPlayerCommands.commands(
            service,
            kernel,
            messages,
            adventure,
            presentation,
        ) + KitAdminCommands.commands(
            service,
            known,
            kernel,
            messages,
            adventure,
            presentation,
        )

}
