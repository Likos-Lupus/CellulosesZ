package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record UserPreferences(
        boolean privateMessages,
        boolean payments,
        boolean teleportRequests,
        boolean teleportAutoAccept,
        boolean confirmLargePayments,
        boolean confirmInventoryClears,
        boolean replyToLastRecipient,
        boolean powerToolsEnabled,
        boolean socialSpy,
        @Nullable UUID incomingReplyTarget,
        @Nullable UUID outgoingReplyTarget
) {

    public static UserPreferences defaults() {
        return new UserPreferences(
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                false,
                null,
                null
        );
    }

    public UserPreferences withPrivateMessages(boolean value) {
        return copy(value, payments, teleportRequests, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    private static UserPreferences copy(
            boolean privateMessages,
            boolean payments,
            boolean teleportRequests,
            boolean teleportAutoAccept,
            boolean confirmLargePayments,
            boolean confirmInventoryClears,
            boolean replyToLastRecipient,
            boolean powerToolsEnabled,
            boolean socialSpy,
            @Nullable UUID incomingReplyTarget,
            @Nullable UUID outgoingReplyTarget
    ) {
        return new UserPreferences(
                privateMessages,
                payments,
                teleportRequests,
                teleportAutoAccept,
                confirmLargePayments,
                confirmInventoryClears,
                replyToLastRecipient,
                powerToolsEnabled,
                socialSpy,
                incomingReplyTarget,
                outgoingReplyTarget
        );
    }

    public UserPreferences withPayments(boolean value) {
        return copy(privateMessages, value, teleportRequests, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withTeleportRequests(boolean value) {
        return copy(privateMessages, payments, value, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withTeleportAutoAccept(boolean value) {
        return copy(privateMessages, payments, teleportRequests, value, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withConfirmLargePayments(boolean value) {
        return copy(privateMessages, payments, teleportRequests, teleportAutoAccept, value, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withConfirmInventoryClears(boolean value) {
        return copy(privateMessages, payments, teleportRequests, teleportAutoAccept, confirmLargePayments, value, replyToLastRecipient, powerToolsEnabled, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withReplyToLastRecipient(boolean value) {
        return copy(privateMessages, payments, teleportRequests, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, value, powerToolsEnabled, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withPowerToolsEnabled(boolean value) {
        return copy(privateMessages, payments, teleportRequests, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, value, socialSpy, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withSocialSpy(boolean value) {
        return copy(privateMessages, payments, teleportRequests, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, value, incomingReplyTarget, outgoingReplyTarget);
    }

    public UserPreferences withIncomingReplyTarget(@Nullable UUID value) {
        return copy(privateMessages, payments, teleportRequests, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, socialSpy, value, outgoingReplyTarget);
    }

    public UserPreferences withOutgoingReplyTarget(@Nullable UUID value) {
        return copy(privateMessages, payments, teleportRequests, teleportAutoAccept, confirmLargePayments, confirmInventoryClears, replyToLastRecipient, powerToolsEnabled, socialSpy, incomingReplyTarget, value);
    }

}
