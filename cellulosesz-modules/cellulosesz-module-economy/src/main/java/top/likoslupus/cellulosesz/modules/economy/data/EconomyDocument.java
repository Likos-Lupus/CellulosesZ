package top.likoslupus.cellulosesz.modules.economy.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EconomyDocument {

    public Map<String, String> balances = new LinkedHashMap<>();
    public List<TransactionLogEntry> transactions = new ArrayList<>();

}
