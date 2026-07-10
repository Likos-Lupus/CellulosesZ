package top.likoslupus.cellulosesz.modules.economy;

public final class EconomyConfig {

    public int schema = 1;
    public Currency currency = new Currency();
    public String startingBalance = "0.00";
    public String minimumBalance = "0.00";
    public String maximumBalance = "1000000000.00";
    public Pay pay = new Pay();
    public BalanceTop balanceTop = new BalanceTop();

    public static final class Currency {

        public String singular = "coin";
        public String plural = "coins";
        public String symbol = "$";
        public int scale = 2;

    }

    public static final class Pay {

        public String minimum = "0.01";
        public String requireConfirmAbove = "10000.00";

    }

    public static final class BalanceTop {

        public int cacheSeconds = 300;
        public int pageSize = 10;

    }

}
