package top.likoslupus.cellulosesz.modules.economy.data;

import java.util.ArrayList;
import java.util.List;

public final class TransactionLogDocument {

    public int schema = 1;
    public List<TransactionLogEntry> entries = new ArrayList<>();

}
