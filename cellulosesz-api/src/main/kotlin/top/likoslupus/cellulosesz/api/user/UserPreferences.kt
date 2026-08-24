package top.likoslupus.cellulosesz.api.user

import java.util.*

@JvmRecord
public data class UserPreferences(
    public val privateMessages: Boolean,
    public val payments: Boolean,
    public val teleportRequests: Boolean,
    public val teleportAutoAccept: Boolean,
    public val confirmLargePayments: Boolean,
    public val confirmInventoryClears: Boolean,
    public val replyToLastRecipient: Boolean,
    public val powerToolsEnabled: Boolean,
    public val socialSpy: Boolean,
    public val incomingReplyTarget: UUID?,
    public val outgoingReplyTarget: UUID?
) {

    public fun withPrivateMessages(value: Boolean): UserPreferences =
        copy(privateMessages = value)

    public fun withPayments(value: Boolean): UserPreferences =
        copy(payments = value)

    public fun withTeleportRequests(value: Boolean): UserPreferences =
        copy(teleportRequests = value)

    public fun withTeleportAutoAccept(value: Boolean): UserPreferences =
        copy(teleportAutoAccept = value)

    public fun withConfirmLargePayments(value: Boolean): UserPreferences =
        copy(confirmLargePayments = value)

    public fun withConfirmInventoryClears(value: Boolean): UserPreferences =
        copy(confirmInventoryClears = value)

    public fun withReplyToLastRecipient(value: Boolean): UserPreferences =
        copy(replyToLastRecipient = value)

    public fun withPowerToolsEnabled(value: Boolean): UserPreferences =
        copy(powerToolsEnabled = value)

    public fun withSocialSpy(value: Boolean): UserPreferences =
        copy(socialSpy = value)

    public fun withIncomingReplyTarget(value: UUID?): UserPreferences =
        copy(incomingReplyTarget = value)

    public fun withOutgoingReplyTarget(value: UUID?): UserPreferences =
        copy(outgoingReplyTarget = value)

    public companion object {

        @JvmStatic
        public fun defaults(): UserPreferences =
            UserPreferences(
                privateMessages = true,
                payments = true,
                teleportRequests = true,
                teleportAutoAccept = false,
                confirmLargePayments = true,
                confirmInventoryClears = true,
                replyToLastRecipient = false,
                powerToolsEnabled = true,
                socialSpy = false,
                incomingReplyTarget = null,
                outgoingReplyTarget = null
            )

    }

}
