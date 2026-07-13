package top.likoslupus.cellulosesz.api.user;

public final class UserPreferences {

    public boolean privateMessages = true;
    public boolean payments = true;
    public boolean teleportRequests = true;
    public boolean confirmLargePayments = true;
    /**
     * When enabled, /reply targets the last player this user messaged rather than the last incoming sender.
     */
    public boolean replyToLastRecipient;
    public boolean powerToolsEnabled = true;

}
