package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class EconomyMessages {

    private EconomyMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.economy.balance-top.invalid-filter", "<red>Invalid filter.");
        messages.put("commands.economy.sell.amount-not-allowed-for-all", "<primary>Amount not allowed for all.");
        messages.put("commands.economy.sell.empty-hand", "<red>Empty hand.");
        messages.put("commands.economy.sell.invalid-amount", "<red>Invalid amount.");
        messages.put("commands.economy.sell.invalid-item", "<red>Invalid item.");
        messages.put("commands.economy.sell.inventory-changed", "<primary>Inventory changed.");
        messages.put("commands.economy.component-item-unsupported", "<primary>Items with custom data components cannot be priced or sold by the base-item price table.");
        messages.put("commands.economy.sell.no-sellable-items", "<primary>No sellable items.");
        messages.put("commands.economy.sell.no-worth", "<primary>No worth.");
        messages.put("commands.economy.sell.not-enough", "<red>Only <secondary>{available}<red> of <secondary>{item}<red> are available; <secondary>{requested}<red> requested.");
        messages.put("commands.economy.sell.player-only", "<primary>Player only.");
        messages.put("commands.economy.sell.rollback-failed", "<red>Rollback failed.");
        messages.put("commands.economy.sell.success", "<primary>Sold <secondary>{count}<primary> item(s) for <secondary>{amount}<primary>.");
        messages.put("commands.economy.worth-batch", "<primary>Worth results (<secondary>{found}<primary> found, total <secondary>{total}<primary>):{rows}");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.economy.balance-top.invalid-filter", "<red>操作失败：Invalid filter。");
        messages.put("commands.economy.sell.amount-not-allowed-for-all", "<primary>Amount not allowed for all。");
        messages.put("commands.economy.sell.empty-hand", "<red>操作失败：Empty hand。");
        messages.put("commands.economy.sell.invalid-amount", "<red>操作失败：Invalid amount。");
        messages.put("commands.economy.sell.invalid-item", "<red>操作失败：Invalid item。");
        messages.put("commands.economy.sell.inventory-changed", "<primary>背包内容已变化。");
        messages.put("commands.economy.component-item-unsupported", "<primary>带自定义数据组件的物品不能按基础物品价格表计价或出售。");
        messages.put("commands.economy.sell.no-sellable-items", "<primary>No sellable items。");
        messages.put("commands.economy.sell.no-worth", "<primary>No worth。");
        messages.put("commands.economy.sell.not-enough", "<red>只有 <secondary>{available}<red> 个 <secondary>{item}<red>，请求了 <secondary>{requested}<red> 个。");
        messages.put("commands.economy.sell.player-only", "<primary>Player only。");
        messages.put("commands.economy.sell.rollback-failed", "<red>操作失败：Rollback failed。");
        messages.put("commands.economy.sell.success", "<primary>已出售 <secondary>{count}<primary> 个物品，获得 <secondary>{amount}<primary>。");
        messages.put("commands.economy.worth-batch", "<primary>价值结果（找到 <secondary>{found}<primary> 项，总计 <secondary>{total}<primary>）：{rows}");
        return Map.copyOf(messages);
    }

}
