package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class HomeMessages {

    private HomeMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.home.abstract-home-command.error.1", "<red>This command can only be used by a player.");
        messages.put("commands.home.abstract-home-command.error.2", "<red>Home names must be between <secondary>{value0}<red> and <secondary>{value1}<red> characters long.");
        messages.put("commands.home.abstract-home-command.error.3", "<red>Home names may only contain the configured characters.");
        messages.put("commands.home.del-home-command.reply.1", "<primary>Deleted home: <secondary>{value0}<primary>");
        messages.put("commands.home.del-home-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.home.del-home-command.error.2", "<red>Home does not exist: <secondary>{value0}<red>");
        messages.put("commands.home.home-command.reply.1", "<primary>Teleported to home: <secondary>{value0}<primary>");
        messages.put("commands.home.home-command.error.1", "<red>Home does not exist: <secondary>{value0}<red>");
        messages.put("commands.home.home-command.error.2", "<red>Teleport failed: <secondary>{value0}<red>");
        messages.put("commands.home.rename-home-command.reply.1", "<primary>Renamed home <secondary>{value0}<primary> to <secondary>{value1}<primary>.");
        messages.put("commands.home.rename-home-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.home.rename-home-command.error.2", "<red>Unable to rename the home; the old name may not exist or the new name may already be in use.");
        messages.put("commands.home.set-home-command.reply.1", "<primary>Set home: <secondary>{value0}<primary>");
        messages.put("commands.home.set-home-command.error.1", "<red>You have reached the home limit: <secondary>{value0}<red>");
        messages.put("commands.home.list-empty", "<primary>You have not set any homes.");
        messages.put("commands.home.cooldown", "<red>You must wait <secondary>{seconds}<red> seconds before teleporting home again.");
        messages.put("commands.home.list", "<primary>Homes: <secondary>{homes}");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.home.abstract-home-command.error.1", "<red>此命令只能由玩家执行。");
        messages.put("commands.home.abstract-home-command.error.2", "<red>Home 名称长度必须在 <secondary>{value0}<red> 到 <secondary>{value1}<red> 之间。");
        messages.put("commands.home.abstract-home-command.error.3", "<red>Home 名称只能包含允许的字符。");
        messages.put("commands.home.del-home-command.reply.1", "<primary>已删除 Home: <secondary>{value0}<primary>");
        messages.put("commands.home.del-home-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.home.del-home-command.error.2", "<red>Home 不存在: <secondary>{value0}<red>");
        messages.put("commands.home.home-command.reply.1", "<primary>已传送到 Home: <secondary>{value0}<primary>");
        messages.put("commands.home.home-command.error.1", "<red>Home 不存在: <secondary>{value0}<red>");
        messages.put("commands.home.home-command.error.2", "<red>传送失败: <secondary>{value0}<red>");
        messages.put("commands.home.rename-home-command.reply.1", "<primary>已将 Home <secondary>{value0}<primary> 重命名为 <secondary>{value1}<primary>");
        messages.put("commands.home.rename-home-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.home.rename-home-command.error.2", "<red>无法重命名 Home，可能旧名称不存在或新名称已存在。");
        messages.put("commands.home.set-home-command.reply.1", "<primary>已设置 Home: <secondary>{value0}<primary>");
        messages.put("commands.home.set-home-command.error.1", "<red>Home 数量已达到上限: <secondary>{value0}<red>");
        messages.put("commands.home.list-empty", "<primary>你还没有设置 Home。");
        messages.put("commands.home.cooldown", "<red>还需等待 <secondary>{seconds}<red> 秒才能再次传送到 Home。");
        messages.put("commands.home.list", "<primary>Home：<secondary>{homes}");
        return Map.copyOf(messages);
    }

}
