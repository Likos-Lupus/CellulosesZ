package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class KitMessages {

    private KitMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.kit.abstract-kit-command.error.1", "<red>This command can only be used by a player.");
        messages.put("commands.kit.create-kit-command.reply.1", "<primary>Created kit: <secondary>{value0}<primary>");
        messages.put("commands.kit.create-kit-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.kit.create-kit-command.error.2", "<red>Invalid item format.");
        messages.put("commands.kit.del-kit-command.reply.1", "<primary>Deleted kit: <secondary>{value0}<primary>");
        messages.put("commands.kit.del-kit-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.kit.del-kit-command.error.2", "<red>Kit does not exist: <secondary>{value0}<red>");
        messages.put("commands.kit.kit-command.error.1", "<red>Kit does not exist: <secondary>{value0}<red>");
        messages.put("commands.kit.kit-command.error.2", "<red>You do not have permission to claim this kit.");
        messages.put("commands.kit.kit-reset-command.reply.1", "<primary>Kit cooldown reset.");
        messages.put("commands.kit.kit-reset-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.kit.kit-reset-command.error.2", "<red>Player not found: <secondary>{value0}<red>");
        messages.put("commands.kit.show-kit-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.kit.show-kit-command.error.2", "<red>Kit does not exist: <secondary>{value0}<red>");
        messages.put("commands.kit.list-empty", "<primary>There are no kits available to you.");
        messages.put("commands.kit.list", "<primary>Kits: <secondary>{kits}");
        messages.put("commands.kit.details", "<primary>Kit <secondary>{kit}<primary>:{entries}");
        messages.put("service.kit.cooldown", "<red>That kit is on cooldown for another <secondary>{seconds}<red> second(s).");
        messages.put("service.kit.economy-unavailable", "<red>This kit requires the economy module, but it is unavailable.");
        messages.put("service.kit.user-not-loaded", "<red>Your player data is still loading; try the kit command again in a moment.");
        messages.put("service.kit.item-failed", "<red>Failed to give kit item: <secondary>{item}<red>");
        messages.put("service.kit.claimed", "<primary>Claimed kit <secondary>{kit}<primary>.");
        messages.put("commands.kit.create-kit-command.error.cooldown", "<red>The cooldown must be zero or a positive number of seconds, or <secondary>once<red>.");
        messages.put("commands.kit.create-kit-command.error.empty", "<red>Your inventory is empty; no kit was created.");
        messages.put("commands.kit.create-kit-command.error.snapshot", "<red>Your inventory could not be serialized; no kit was created.");
        messages.put("commands.kit.show-kit-command.error.invalid-item", "<red>The kit contains an invalid serialized item stack.");
        messages.put("commands.kit.kit-reset-command.error.kit", "<red>Kit not found: <secondary>{kit}<red>");
        messages.put("commands.kit.kit-reset-command.error.player-required", "<red>Console must specify a player.");
        messages.put("commands.kit.kit-reset-command.error.others", "<red>You do not have permission to reset another player’s kit cooldown.");
        messages.put("service.kit.persistence-failed", "<red>The kit data could not be persisted; the change was rolled back.");
        messages.put("service.kit.inventory-unavailable", "<red>The kit requires inventory slots that are not currently empty.");
        messages.put("service.kit.inventory-changed", "<red>Your inventory changed before the kit could be granted.");
        messages.put("service.kit.rollback-failed", "<red>The kit claim failed and could not be fully rolled back; contact an administrator.");
        messages.put("service.kit.once", "<red>This one-time kit has already been claimed.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.kit.abstract-kit-command.error.1", "<red>此命令只能由玩家执行。");
        messages.put("commands.kit.create-kit-command.reply.1", "<primary>已创建 Kit: <secondary>{value0}<primary>");
        messages.put("commands.kit.create-kit-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.kit.create-kit-command.error.2", "<red>物品格式错误。");
        messages.put("commands.kit.del-kit-command.reply.1", "<primary>已删除 Kit: <secondary>{value0}<primary>");
        messages.put("commands.kit.del-kit-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.kit.del-kit-command.error.2", "<red>Kit 不存在: <secondary>{value0}<red>");
        messages.put("commands.kit.kit-command.error.1", "<red>Kit 不存在: <secondary>{value0}<red>");
        messages.put("commands.kit.kit-command.error.2", "<red>你没有权限领取此 Kit。");
        messages.put("commands.kit.kit-reset-command.reply.1", "<primary>已重置 Kit 冷却。");
        messages.put("commands.kit.kit-reset-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.kit.kit-reset-command.error.2", "<red>找不到玩家: <secondary>{value0}<red>");
        messages.put("commands.kit.show-kit-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.kit.show-kit-command.error.2", "<red>Kit 不存在: <secondary>{value0}<red>");
        messages.put("commands.kit.list-empty", "<primary>当前没有可用 Kit。");
        messages.put("commands.kit.list", "<primary>Kit：<secondary>{kits}");
        messages.put("commands.kit.details", "<primary>Kit <secondary>{kit}<primary>：{entries}");
        messages.put("service.kit.cooldown", "<red>Kit 仍在冷却中，剩余 <secondary>{seconds}<red> 秒。");
        messages.put("service.kit.economy-unavailable", "<red>该 Kit 需要经济模块，但经济模块当前不可用。");
        messages.put("service.kit.user-not-loaded", "<red>你的玩家数据仍在加载，请稍后重新执行 Kit 命令。");
        messages.put("service.kit.item-failed", "<red>发放 Kit 物品失败：<secondary>{item}<red>");
        messages.put("service.kit.claimed", "<primary>已领取 Kit：<secondary>{kit}<primary>");
        messages.put("commands.kit.create-kit-command.error.cooldown", "<red>冷却必须为 0 或正整数秒，也可填写 <secondary>once<red>。");
        messages.put("commands.kit.create-kit-command.error.empty", "<red>背包为空，未创建 Kit。");
        messages.put("commands.kit.create-kit-command.error.snapshot", "<red>无法完整序列化你的背包，未创建 Kit。");
        messages.put("commands.kit.show-kit-command.error.invalid-item", "<red>该 Kit 包含无法解析的物品栈数据。");
        messages.put("commands.kit.kit-reset-command.error.kit", "<red>Kit 不存在：<secondary>{kit}<red>");
        messages.put("commands.kit.kit-reset-command.error.player-required", "<red>控制台必须指定玩家。");
        messages.put("commands.kit.kit-reset-command.error.others", "<red>你没有权限重置其他玩家的 Kit 冷却。");
        messages.put("service.kit.persistence-failed", "<red>Kit 数据无法持久化，本次变更已回滚。");
        messages.put("service.kit.inventory-unavailable", "<red>该 Kit 所需的原始背包槽位当前并非全部为空。");
        messages.put("service.kit.inventory-changed", "<red>发放 Kit 前背包状态发生变化。");
        messages.put("service.kit.rollback-failed", "<red>Kit 领取失败且未能完整回滚，请联系管理员。");
        messages.put("service.kit.once", "<red>该一次性 Kit 已经领取过。");
        return Map.copyOf(messages);
    }

}
