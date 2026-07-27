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
        messages.put("commands.common.no-permission", "<red>You do not have permission for this operation.");
        messages.put("commands.common.platform.invalid-argument", "<red>The platform rejected the supplied parameters.");
        messages.put("commands.common.platform.target-not-found", "<red>The platform could not find the requested target.");
        messages.put("commands.common.platform.state-not-allowed", "<red>The current game state does not allow this operation.");
        messages.put("commands.common.platform.exempt", "<red>The target is exempt from this operation.");
        messages.put("commands.common.platform.unsupported", "<red>This operation is not supported by the current platform.");
        messages.put("commands.common.platform.conflict", "<red>The target changed before the operation could be committed; nothing was changed.");
        messages.put("commands.common.platform.partial-success", "<red>The platform completed only part of the requested operation.");
        messages.put("commands.common.platform.rollback-failed", "<red>The operation failed and rollback was incomplete; contact an administrator.");
        messages.put("commands.common.platform.internal-error", "<red>The platform could not complete the operation because of an internal error.");
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
        messages.put("commands.common.no-permission", "<red>你没有执行此操作所需的权限。");
        messages.put("commands.common.platform.invalid-argument", "<red>平台拒绝了提供的参数。");
        messages.put("commands.common.platform.target-not-found", "<red>平台找不到请求的目标。");
        messages.put("commands.common.platform.state-not-allowed", "<red>当前游戏状态不允许执行此操作。");
        messages.put("commands.common.platform.exempt", "<red>目标已被豁免，无法执行此操作。");
        messages.put("commands.common.platform.unsupported", "<red>当前平台不支持此操作。");
        messages.put("commands.common.platform.conflict", "<red>提交前目标状态已变化，本次未进行修改。");
        messages.put("commands.common.platform.partial-success", "<red>平台只完成了请求中的部分操作。");
        messages.put("commands.common.platform.rollback-failed", "<red>操作失败且回滚不完整，请联系管理员。");
        messages.put("commands.common.platform.internal-error", "<red>平台发生内部错误，无法完成此操作。");
        return Map.copyOf(messages);
    }

}
