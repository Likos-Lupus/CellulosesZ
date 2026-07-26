package top.likoslupus.cellulosesz.modules.economy;

public final class EconomyConfig {

    public Currency currency = new Currency();
    public String startingBalance = "0.00";
    public String minimumBalance = "0.00";
    public String maximumBalance = "1000000000.00";
    public Pay pay = new Pay();
    public BalanceTop balanceTop = new BalanceTop();

    public void copyFrom(EconomyConfig source) {
        currency.copyFrom(source.currency);
        startingBalance = source.startingBalance;
        minimumBalance = source.minimumBalance;
        maximumBalance = source.maximumBalance;
        pay.copyFrom(source.pay);
        balanceTop.copyFrom(source.balanceTop);
    }

    public static final class Currency {

        public String singular = "coin";
        public String plural = "coins";
        public String symbol = "$";
        public boolean symbolBefore = true;
        public boolean spaceBetweenSymbolAndAmount;
        public boolean grouping = true;
        public boolean showName;
        public int scale = 2;

        private void copyFrom(Currency source) {
            singular = source.singular;
            plural = source.plural;
            symbol = source.symbol;
            symbolBefore = source.symbolBefore;
            spaceBetweenSymbolAndAmount = source.spaceBetweenSymbolAndAmount;
            grouping = source.grouping;
            showName = source.showName;
            scale = source.scale;
        }

    }

    public static final class Pay {

        public String minimum = "0.01";
        public String requireConfirmAbove = "10000.00";
        public boolean respectIgnore = true;
        public boolean allowOfflineByDefault;
        public int maximumRecipients = 10;

        private void copyFrom(Pay source) {
            minimum = source.minimum;
            requireConfirmAbove = source.requireConfirmAbove;
            respectIgnore = source.respectIgnore;
            allowOfflineByDefault = source.allowOfflineByDefault;
            maximumRecipients = source.maximumRecipients;
        }

    }

    public static final class BalanceTop {

        public int cacheSeconds = 300;
        public int pageSize = 10;

        private void copyFrom(BalanceTop source) {
            cacheSeconds = source.cacheSeconds;
            pageSize = source.pageSize;
        }

    }

}
