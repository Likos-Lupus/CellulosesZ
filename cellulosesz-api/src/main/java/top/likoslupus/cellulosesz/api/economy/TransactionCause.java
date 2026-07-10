package top.likoslupus.cellulosesz.api.economy;

public record TransactionCause(
        String type,
        String actor,
        String note
) {

    public static TransactionCause command(String actor, String note) {
        return new TransactionCause("command", actor, note);
    }

    public static TransactionCause system(String note) {
        return new TransactionCause("system", "system", note);
    }

}
