package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class CommonMessages {

    private CommonMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("common.console", "Console");
        messages.put("service.user.persistence-failed", "<red>The user setting could not be persisted; the change was rolled back.");
        messages.put("commands.common.invalid-boolean", "<red>Invalid boolean.");
        messages.put("commands.common.invalid-page", "<red>Invalid page.");
        messages.put("commands.common.page-out-of-range", "<red>Page out of range; last page: <secondary>{pages}<red>.");
        messages.put("commands.common.player-offline", "<red>Player is offline: <secondary>{player}<red>.");
        messages.put("commands.common.player-required", "<red>A player target is required: <secondary>{player}<red>.");
        messages.put("commands.common.unknown-player", "<red>Unknown player: <secondary>{player}<red>.");
        messages.put("common.persistence-failed", "<red>Persistence failed.");
        messages.put("common.command-cost-failed", "<red>The command cost of <secondary>{cost}<red> could not be charged.");
        messages.put("common.console-only", "<red>This command can only be used from the console.");
        messages.put("common.module-disabled", "<red>The required module is disabled.");
        messages.put("common.no-permission", "<red>You do not have permission to do that.");
        messages.put("common.player-only", "<red>This command can only be used by a player.");
        messages.put("common.usage", "<red>Usage: <secondary>{usage}<red>");
        messages.put("service.user.load-failed", "<red>Player data could not be loaded.");
        messages.put("service.user.rollback-failed", "<red>The operation failed and player data could not be fully rolled back. Contact an administrator.");
        messages.put("common.player-not-found", "<red>Player not found: <secondary>{player}");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("common.console", "控制台");
        messages.put("service.user.persistence-failed", "<red>用户设置无法持久化，本次变更已回滚。");
        messages.put("commands.common.invalid-boolean", "<red>操作失败：Invalid boolean。");
        messages.put("commands.common.invalid-page", "<red>操作失败：Invalid page。");
        messages.put("commands.common.page-out-of-range", "<red>页码超出范围，最后一页为 <secondary>{pages}<red>。");
        messages.put("commands.common.player-offline", "<red>玩家不在线：<secondary>{player}<red>。");
        messages.put("commands.common.player-required", "<red>需要指定玩家：<secondary>{player}<red>。");
        messages.put("commands.common.unknown-player", "<red>未知玩家：<secondary>{player}<red>。");
        messages.put("common.persistence-failed", "<red>操作失败：Persistence failed。");
        messages.put("common.command-cost-failed", "<red>无法扣除命令费用 <secondary>{cost}<red>。");
        messages.put("common.console-only", "<red>此命令只能由控制台使用。");
        messages.put("common.module-disabled", "<red>所需模块当前未启用。");
        messages.put("common.no-permission", "<red>你没有执行此操作的权限。");
        messages.put("common.player-only", "<red>此命令只能由玩家使用。");
        messages.put("common.usage", "<red>用法：<secondary>{usage}<red>");
        messages.put("service.user.load-failed", "<red>无法加载玩家数据。");
        messages.put("service.user.rollback-failed", "<red>操作失败且玩家数据未能完整回滚，请联系管理员。");
        messages.put("common.player-not-found", "<red>找不到玩家：<secondary>{player}");
        return Map.copyOf(messages);
    }

}
