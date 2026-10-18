package top.likoslupus.cellulosesz.administration.jail

import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/**
 * `/setjail`, `/jail`, `/unjail`, `/togglejail`, `/deljail`, `/jails` and `/jailedplayers`.
 *
 * The declarations live next to their handlers, split between [JailAdminCommands] and
 * [JailPlayerCommands]; this entry point only assembles them in registration order.
 */
internal object JailCommands {

    fun commands(
        jail: JailService,
        notifier: ModerationNotifier,
        moderationSettings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = buildList {
        add(
            JailAdminCommands.setJail(
                jail,
                kernel,
                messages,
                adventure,
                presentation,
            )
        )
        addAll(
            JailPlayerCommands.commands(
                jail,
                notifier,
                moderationSettings,
                kernel,
                messages,
                adventure,
                presentation,
            )
        )
        addAll(
            JailAdminCommands.commands(
                jail,
                kernel,
                messages,
                adventure,
                presentation,
            )
        )
    }

}
