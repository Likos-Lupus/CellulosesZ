package top.likoslupus.cellulosesz.modules.economy.data;

import org.jspecify.annotations.Nullable;

public final class TransactionLogEntry {

    public long at = System.currentTimeMillis();
    public String causeType = "system";
    public String actor = "system";
    public String note = "";
    public String amount = "0.00";
    public @Nullable String from;
    public @Nullable String to;
    public boolean success;
    public String message = "";

}
