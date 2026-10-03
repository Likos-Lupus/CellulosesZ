package top.likoslupus.cellulosesz.communication.mail

import java.util.*

/** The immutable origin of a mail message. */
internal sealed interface MailSender {

    data class Player(
        val id: UUID,
        val name: String,
    ) : MailSender

    data object Console : MailSender

}

internal val MailSender.displayName: String
    get() = when (this) {
        MailSender.Console -> "Console"
        is MailSender.Player -> name
    }
