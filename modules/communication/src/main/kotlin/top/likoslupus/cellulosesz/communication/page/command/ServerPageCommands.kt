package top.likoslupus.cellulosesz.communication.page.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.page.ServerPageResult
import top.likoslupus.cellulosesz.communication.page.ServerPageService
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.InformationOutput
import top.likoslupus.cellulosesz.core.text.document.AdventureDocumentRenderer
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices

/**
 * `/motd`, `/rules`, `/info` and `/customtext <page>`: whole-document Markdown server pages rendered
 * per recipient language. Pages are read on demand, so a file edit takes effect on the next view.
 */
internal object ServerPageCommands {

    fun commands(
        pages: ServerPageService,
        languages: LanguageResolver,
        theme: () -> DocumentTheme,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "motd",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.MOTD,
            documentation = "communication/motd",
        ) {
            executes {
                show(
                    source,
                    "motd",
                    true,
                    pages,
                    languages,
                    theme,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "rules",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.RULES,
            documentation = "communication/rules",
        ) {
            executes {
                show(
                    source,
                    "rules",
                    true,
                    pages,
                    languages,
                    theme,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "info",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.INFO,
            documentation = "communication/info",
        ) {
            executes {
                show(
                    source,
                    "info",
                    true,
                    pages,
                    languages,
                    theme,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "customtext",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.CUSTOMTEXT,
            documentation = "communication/customtext",
        ) {
            argument("page", word()) { key ->
                executes {
                    show(
                        source,
                        get(key),
                        false,
                        pages,
                        languages,
                        theme,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun show(
        source: CommandSourceStack,
        pageName: String,
        fixed: Boolean,
        pages: ServerPageService,
        languages: LanguageResolver,
        theme: () -> DocumentTheme,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val playerId = source.player?.uuid
        val language = languages.effective(playerId)
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.loading_page"),
        ) { target ->
            when (val result = pages.load(pageName, language)) {
                is ServerPageResult.Found -> {
                    val lines = AdventureDocumentRenderer(theme()).render(result.document)
                    InformationOutput(adventure, kernel).deliver(target, lines)
                }

                else -> kernel.sendNotices(
                    target,
                    adventure,
                    presentation,
                    playerId,
                    listOf(CommunicationMessages.pageFeedback(result, pageName, fixed)),
                )
            }
        }
    }

}
