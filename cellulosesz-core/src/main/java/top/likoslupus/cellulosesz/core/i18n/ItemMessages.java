package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class ItemMessages {

    private ItemMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.item.blacklisted", "<primary>Blacklisted.");
        messages.put("commands.item.firework.cleared", "<primary>Cleared.");
        messages.put("commands.item.firework.effect", "<primary>Effect.");
        messages.put("commands.item.firework.failed", "<red>Failed.");
        messages.put("commands.item.firework.power", "<primary>Power.");
        messages.put("commands.item.firework.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.item.held-item-required", "<primary>Held item required.");
        messages.put("commands.item.invalid-item", "<red>Invalid item.");
        messages.put("commands.item.itemlore.cleared", "<primary>Cleared.");
        messages.put("commands.item.itemlore.invalid", "<red>Invalid.");
        messages.put("commands.item.itemlore.set", "<primary>Set.");
        messages.put("commands.item.itemlore.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.item.itemname.cleared", "<primary>Cleared.");
        messages.put("commands.item.itemname.set", "<primary>Set.");
        messages.put("commands.item.itemname.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.item.oversized", "<primary>Oversized.");
        messages.put("commands.item.player-only", "<primary>Player only.");
        messages.put("commands.item.potion.cleared", "<primary>Cleared.");
        messages.put("commands.item.potion.failed", "<red>Failed.");
        messages.put("commands.item.potion.invalid-effect", "<red>Invalid effect.");
        messages.put("commands.item.potion.invalid-number", "<red>Invalid number.");
        messages.put("commands.item.potion.set", "<primary>Set.");
        messages.put("commands.item.potion.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.item.workstation.failed", "<red>Failed.");
        messages.put("commands.item.workstation.opened", "<primary>Opened.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.item.blacklisted", "<primary>Blacklisted。");
        messages.put("commands.item.firework.cleared", "<primary>操作成功：Cleared。");
        messages.put("commands.item.firework.effect", "<primary>Effect。");
        messages.put("commands.item.firework.failed", "<red>操作失败：Failed。");
        messages.put("commands.item.firework.power", "<primary>Power。");
        messages.put("commands.item.firework.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.item.held-item-required", "<primary>Held item required。");
        messages.put("commands.item.invalid-item", "<red>操作失败：Invalid item。");
        messages.put("commands.item.itemlore.cleared", "<primary>操作成功：Cleared。");
        messages.put("commands.item.itemlore.invalid", "<red>操作失败：Invalid。");
        messages.put("commands.item.itemlore.set", "<primary>操作成功：Set。");
        messages.put("commands.item.itemlore.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.item.itemname.cleared", "<primary>操作成功：Cleared。");
        messages.put("commands.item.itemname.set", "<primary>操作成功：Set。");
        messages.put("commands.item.itemname.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.item.oversized", "<primary>Oversized。");
        messages.put("commands.item.player-only", "<primary>Player only。");
        messages.put("commands.item.potion.cleared", "<primary>操作成功：Cleared。");
        messages.put("commands.item.potion.failed", "<red>操作失败：Failed。");
        messages.put("commands.item.potion.invalid-effect", "<red>操作失败：Invalid effect。");
        messages.put("commands.item.potion.invalid-number", "<red>操作失败：Invalid number。");
        messages.put("commands.item.potion.set", "<primary>操作成功：Set。");
        messages.put("commands.item.potion.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.item.workstation.failed", "<red>操作失败：Failed。");
        messages.put("commands.item.workstation.opened", "<primary>操作成功：Opened。");
        return Map.copyOf(messages);
    }

}
