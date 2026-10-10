package top.likoslupus.cellulosesz.communication.mail.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.mail.*
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.time.DurationParser

/** `/mail` summary, `/mail read`, `/mail send`, `/mail sendtemp` and `/mail clear`. */
internal object MailCommands {

    fun commands(
        mail: MailService,
        kernel: RuntimeKernel,
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
                    kernel
                )
            }
            literal("read") {
                executes {
                    read(
                        context,
                        mail,
                        kernel,
                        1
                    )
                }
                argument("page", integer(1)) { page ->
                    executes {
                        read(
                            context,
                            mail,
                            kernel,
                            get(page)
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
                                null
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
                                    kernel
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
                        kernel
                    )
                }
            }
        },
    )

    private fun summary(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val owner = owner(source)
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        return communicationLaunch(
            source,
            kernel,
            "loading mail..."
        ) {
            kernel.message(
                it,
                CommunicationMessages.mailSummary(mail.summary(owner))
            )
        }
    }

    private fun read(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
        page: Int,
    ): Int {
        val source = context.source
        val owner = owner(source)
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        return communicationLaunch(
            source,
            kernel,
            "loading mail..."
        ) {
            when (val view = mail.read(owner, page)) {
                is MailViewResult.Loaded -> {
                    kernel.message(
                        it,
                        CommunicationMessages.mailHeader(view)
                    )
                    view.page.forEachIndexed { offset, message ->
                        val index = (view.pageIndex - 1) * PAGE_SIZE + offset + 1
                        kernel.message(
                            it,
                            CommunicationMessages.mailLine(index, message)
                        )
                    }
                }

                MailViewResult.Empty ->
                    kernel.message(
                        it,
                        CommunicationMessages.mailEmpty()
                    )

                MailViewResult.StorageUnavailable ->
                    kernel.message(
                        it,
                        CommunicationMessages.mailStorageUnavailable()
                    )
            }
        }
    }

    private fun send(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
        duration: java.time.Duration?,
    ): Int {
        val source = context.source
        val sender = sender(source)
        val senderName = sender.displayName
        val target = StringArgumentType.getString(context, "player")
        val body = StringArgumentType.getString(context, "message")
        return communicationLaunch(
            source,
            kernel,
            "sending mail..."
        ) {
            val result = mail.send(
                sender,
                target,
                body,
                duration
            )
            kernel.message(
                it,
                CommunicationMessages.mailSendFeedback(result)
            )
            if (result is MailSendResult.Sent) {
                kernel.messagePlayer(
                    result.target.id,
                    CommunicationMessages.newMailNotice(senderName),
                )
            }
        }
    }

    private fun sendTemp(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val rawDuration = StringArgumentType.getString(context, "duration")
        val duration = DurationParser.parse(rawDuration)
            ?: return source.replyError(
                CommunicationMessages.prefixed(
                    "invalid duration; use s, m, h, d, w (for example 1d12h)"
                )
            )
        return send(
            context,
            mail,
            kernel,
            duration.duration
        )
    }

    private fun clear(
        context: CommandContext<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val owner = owner(source)
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        return communicationLaunch(
            source,
            kernel,
            "clearing mail..."
        ) { target ->
            val component = when (mail.clear(owner)) {
                MailClearResult.Cleared -> CommunicationMessages.mailCleared()
                MailClearResult.StorageUnavailable -> CommunicationMessages.mailStorageUnavailable()
            }
            kernel.message(target, component)
        }
    }

    private fun owner(source: CommandSourceStack): KnownPlayerIdentity? =
        source.player?.let {
            KnownPlayerIdentity(
                it.uuid,
                it.gameProfile.name
            )
        }

    private fun sender(source: CommandSourceStack): MailSender =
        source.player?.let {
            MailSender.Player(
                it.uuid,
                it.gameProfile.name
            )
        } ?: MailSender.Console

    private const val PAGE_SIZE: Int = 10

}
