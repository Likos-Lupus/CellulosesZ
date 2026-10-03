package top.likoslupus.cellulosesz.communication.mail.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.mail.*
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.time.DurationParser

/** `/mail` summary, `/mail read`, `/mail send`, `/mail sendtemp` and `/mail clear`. */
internal object MailCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        mail: MailService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("mail")
                    .executes { context -> summary(context, mail, kernel) }
                    .then(
                        Commands.literal("read")
                                .executes { context -> read(context, mail, kernel, 1) }
                                .then(
                                    Commands.argument("page", IntegerArgumentType.integer(1))
                                            .executes { context ->
                                                read(
                                                    context,
                                                    mail,
                                                    kernel,
                                                    IntegerArgumentType.getInteger(context, "page")
                                                )
                                            }
                                )
                    )
                    .then(
                        Commands.literal("send")
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                            .then(
                                                Commands.argument(
                                                    "message",
                                                    StringArgumentType.greedyString()
                                                )
                                                        .executes { context ->
                                                            send(context, mail, kernel, null)
                                                        }
                                            )
                                )
                    )
                    .then(
                        Commands.literal("sendtemp")
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                            .then(
                                                Commands.argument(
                                                    "duration",
                                                    StringArgumentType.word()
                                                )
                                                        .then(
                                                            Commands.argument(
                                                                "message",
                                                                StringArgumentType.greedyString()
                                                            )
                                                                    .executes { context ->
                                                                        sendTemp(
                                                                            context,
                                                                            mail,
                                                                            kernel
                                                                        )
                                                                    }
                                                        )
                                            )
                                )
                    )
                    .then(
                        Commands.literal("clear")
                                .executes { context -> clear(context, mail, kernel) }
                    )
        )
    }

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
        return send(context, mail, kernel, duration.duration)
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
