package top.likoslupus.cellulosesz.administration.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.MessageSupport
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

/**
 * Maps administration domain results to semantic values. It holds no language and no colour: the
 * presentation layer renders per recipient. Admin-authored text (reasons) always enters as a
 * [TextArgument.Literal] so it can never be re-parsed as a template.
 */
internal object AdministrationMessages : MessageSupport() {


    internal fun player(value: String): TextArgument =
        TextArgument.PlayerName(value)

    internal fun elapsed(expiresAt: Instant, now: Instant): TextArgument =
        TextArgument.Elapsed(
            Duration.between(now, expiresAt)
                    .coerceAtLeast(Duration.ZERO)
                    .toKotlinDuration()
        )

    internal fun elapsed(value: Duration): TextArgument =
        TextArgument.Elapsed(
            value.coerceAtLeast(Duration.ZERO).toKotlinDuration()
        )

    internal fun withAudit(notice: Notice, recorded: Boolean): Notice =
        when {
            recorded -> notice
            else -> notice.copy(
                detail = LocalizedText(
                    MessageKey("administration.error.audit_warning")
                )
            )
        }

    internal fun remaining(expiresAt: Instant?, now: Instant): LocalizedText =
        when (expiresAt) {
            null -> LocalizedText(MessageKey("administration.muteinfo.permanent"))
            else -> LocalizedText(
                MessageKey("administration.muteinfo.remaining"),
                listOf(elapsed(expiresAt, now))
            )
        }

}
