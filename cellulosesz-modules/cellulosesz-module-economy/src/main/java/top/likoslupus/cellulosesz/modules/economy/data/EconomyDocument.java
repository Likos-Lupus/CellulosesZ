package top.likoslupus.cellulosesz.modules.economy.data;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EconomyDocument {

    public int schema = 1;
    public Map<String, String> balances = new LinkedHashMap<>();

}
