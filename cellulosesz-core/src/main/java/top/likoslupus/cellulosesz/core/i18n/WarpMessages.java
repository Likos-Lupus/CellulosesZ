package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class WarpMessages {

    private WarpMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.warp.abstract-warp-command.error.1", "<red>This command can only be used by a player.");
        messages.put("commands.warp.abstract-warp-command.error.2", "<red>Warp names cannot be empty or longer than <secondary>{value0}<red> characters.");
        messages.put("commands.warp.abstract-warp-command.error.3", "<red>Warp names may only contain the configured characters.");
        messages.put("commands.warp.del-warp-command.reply.1", "<primary>Deleted warp: <secondary>{value0}<primary>");
        messages.put("commands.warp.del-warp-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.warp.del-warp-command.error.2", "<red>Warp does not exist: <secondary>{value0}<red>");
        messages.put("commands.warp.set-warp-command.reply.1", "<primary>Set warp: <secondary>{value0}<primary>");
        messages.put("commands.warp.set-warp-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-command.reply.1", "<primary>Teleported to warp: <secondary>{value0}<primary>");
        messages.put("commands.warp.warp-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-command.error.2", "<red>Warp does not exist: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-command.error.3", "<red>You do not have permission to use this warp.");
        messages.put("commands.warp.warp-command.error.4", "<red>Teleport failed: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-info-command.reply.1", "<primary>Warp <secondary>{value0}<primary> is at <secondary>{value1}<primary>.");
        messages.put("commands.warp.warp-info-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-info-command.error.2", "<red>Warp does not exist: <secondary>{value0}<red>");
        messages.put("commands.warp.list-empty", "<primary>There are no warps.");
        messages.put("commands.warp.cooldown", "<red>You must wait <secondary>{seconds}<red> seconds before using another warp.");
        messages.put("commands.warp.list", "<primary>Warps: <secondary>{warps}");
        messages.put("commands.warp.list-page", "<primary>Warps (<secondary>{page}<primary>/<secondary>{pages}<primary>): <secondary>{warps}<primary>");
        messages.put("commands.warp.set-warp-command.error.exists", "<red>Warp <secondary>{warp}<red> already exists and you do not have overwrite permission.");
        messages.put("service.warp.persistence-failed", "<red>The warp data could not be persisted; the change was rolled back.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.warp.abstract-warp-command.error.1", "<red>此命令只能由玩家执行。");
        messages.put("commands.warp.abstract-warp-command.error.2", "<red>Warp 名称不能为空，且长度不能超过 <secondary>{value0}<red>。");
        messages.put("commands.warp.abstract-warp-command.error.3", "<red>Warp 名称只能包含允许的字符。");
        messages.put("commands.warp.del-warp-command.reply.1", "<primary>已删除 Warp: <secondary>{value0}<primary>");
        messages.put("commands.warp.del-warp-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.warp.del-warp-command.error.2", "<red>Warp 不存在: <secondary>{value0}<red>");
        messages.put("commands.warp.set-warp-command.reply.1", "<primary>已设置 Warp: <secondary>{value0}<primary>");
        messages.put("commands.warp.set-warp-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-command.reply.1", "<primary>已传送到 Warp: <secondary>{value0}<primary>");
        messages.put("commands.warp.warp-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-command.error.2", "<red>Warp 不存在: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-command.error.3", "<red>你没有权限使用此 Warp。");
        messages.put("commands.warp.warp-command.error.4", "<red>传送失败: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-info-command.reply.1", "<primary>Warp <secondary>{value0}<primary> 位于 <secondary>{value1}<primary>");
        messages.put("commands.warp.warp-info-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.warp.warp-info-command.error.2", "<red>Warp 不存在: <secondary>{value0}<red>");
        messages.put("commands.warp.list-empty", "<primary>当前没有 Warp。");
        messages.put("commands.warp.cooldown", "<red>还需等待 <secondary>{seconds}<red> 秒才能再次使用 Warp。");
        messages.put("commands.warp.list", "<primary>Warp：<secondary>{warps}");
        messages.put("commands.warp.list-page", "<primary>Warp（<secondary>{page}<primary>/<secondary>{pages}<primary>）：<secondary>{warps}<primary>");
        messages.put("commands.warp.set-warp-command.error.exists", "<red>Warp <secondary>{warp}<red> 已存在，且你没有覆盖权限。");
        messages.put("service.warp.persistence-failed", "<red>Warp 数据无法持久化，本次变更已回滚。");
        return Map.copyOf(messages);
    }

}
