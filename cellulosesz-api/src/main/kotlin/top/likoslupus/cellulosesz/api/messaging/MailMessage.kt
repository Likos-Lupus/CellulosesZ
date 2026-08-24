package top.likoslupus.cellulosesz.api.messaging

import top.likoslupus.cellulosesz.api.validation.requireNonBlank
import top.likoslupus.cellulosesz.api.validation.requirePositive
import java.util.*

@JvmRecord
public data class MailMessage(
    public val id: UUID,
    public val fromUuid: UUID?,
    public val fromName: String,
    public val toUuid: UUID,
    public val message: String,
    public val sentAt: Long,
    public val expiresAt: Long?,
    public val read: Boolean
) {

    init {
        fromName.requireNonBlank { "fromName" }
        message.requireNonBlank { "message" }
        sentAt.requirePositive { "sentAt" }
        require(!(expiresAt != null && expiresAt <= sentAt)) {
            "expiresAt must be after sentAt"
        }
    }

    public fun expired(now: Long): Boolean =
        expiresAt != null && expiresAt <= now

    public fun withRead(value: Boolean): MailMessage =
        copy(read = value)

}
