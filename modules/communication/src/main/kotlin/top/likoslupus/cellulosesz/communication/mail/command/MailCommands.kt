package top.likoslupus.cellulosesz.communication.mail.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.mail.*
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.core.text.presentation.sendPage
import top.likoslupus.cellulosesz.foundation.time.DurationParser

/** `/mail` summary, `/mail read`, `/mail send`, `/mail sendtemp` and `/mail clear`. */
internal object MailCommands {

    fun commands(
        mail: MailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "mail",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.MAIL,
            documentation = "communication/mail",
        ) {
            executes {
                summary(
                    context,
                    mail,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
            literal("read") {
                executes {
                    read(
                        context,
                        mail,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        1,
                    )
                }
                argument("page", integer(1)) { page ->
                    executes {
                        read(
                            context,
                            mail,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                            get(page),
                        )
                    }
                }
            }
            literal("send") {
                argument("player", word()) {
                    argument("message", greedyString()) {
                        executes {
                            send(
                                context,
                                mail,
                                kernel,
                                messages,
                                adventure,
                                presentation,
                                null,
                            )
                        }
                    }
                }
            }
            literal("sendtemp") {
                argument("player", word()) {
                    argument("duration", word()) {
                        argument("message", greedyString()) {
                            executes {
                                sendTemp(
                                    context,
                                    mail,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation,
                                )
                            }
                        }
                    }
                }
            }
            literal("clear") {
                executes {
                    clear(
                        context,
                        mail,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun summary(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val owner = owner(source)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.loading_mail"),
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                owner.id,
                listOf(CommunicationMessages.mailSummary(mail.summary(owner))),
            )
        }
    }

    private fun read(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        page: Int,
    ): Int {
        val source = context.source
        val owner = owner(source)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.loading_mail"),
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                owner.id,
                CommunicationMessages.mailView(
                    mail.read(
                        owner,
                        page
                    )
                ),
            )
        }
    }

    private fun send(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        duration: java.time.Duration?,
    ): Int {
        val source = context.source
        val sender = sender(source)
        val senderId = (sender as? MailSender.Player)?.id
        val senderName = sender.displayName
        val target = StringArgumentType.getString(context, "player")
        val body = StringArgumentType.getString(context, "message")
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.sending_mail"),
        ) { feedback ->
            val result = mail.send(
                sender,
                target,
                body,
                duration,
            )
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                senderId,
                listOf(CommunicationMessages.mailSendFeedback(result)),
            )
            if (result is MailSendResult.Sent) {
                kernel.sendNotices(
                    CommandFeedbackTarget.Player(result.target.id),
                    adventure,
                    presentation,
                    result.target.id,
                    listOf(CommunicationMessages.newMailNotice(senderName)),
                )
            }
        }
    }

    private fun sendTemp(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val rawDuration = StringArgumentType.getString(context, "duration")
        val duration = DurationParser.parse(rawDuration)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    source.player?.uuid,
                    MessageKey("communication.mail.invalid_duration")
                ),
            )
        return send(
            context,
            mail,
            kernel,
            messages,
            adventure,
            presentation,
            duration.duration,
        )
    }

    private fun clear(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val owner = owner(source)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.clearing_mail"),
        ) { target ->
            val notice = when (mail.clear(owner)) {
                MailClearResult.Cleared -> CommunicationMessages.mailCleared()
                MailClearResult.StorageUnavailable -> CommunicationMessages.mailStorageUnavailable()
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                owner.id,
                listOf(notice)
            )
        }
    }

    private fun owner(source: CommandSourceStack): KnownPlayerIdentity? =
        source.player?.let {
            KnownPlayerIdentity(
                it.uuid,
                it.gameProfile.name,
            )
        }

    private fun sender(source: CommandSourceStack): MailSender =
        source.player?.let {
            MailSender.Player(
                it.uuid,
                it.gameProfile.name,
            )
        } ?: MailSender.Console

}
