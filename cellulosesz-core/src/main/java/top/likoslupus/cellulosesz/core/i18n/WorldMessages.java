package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class WorldMessages {

    private WorldMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.world.remove-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.world.time-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.world.time-command.error.2", "<red>Invalid time format: <secondary>{value0}<red>");
        messages.put("commands.world.weather-command.error.1", "<red>Usage: <secondary>{value0}<red>");
        messages.put("commands.world.weather-command.error.2", "<red>Unknown weather type: <secondary>{value0}<red>");
        messages.put("service.world.time-set", "<primary>Set the time in <secondary>{world}<primary> to <secondary>{time}<primary>.");
        messages.put("service.world.time-failed", "<red>Failed to set the time in <secondary>{world}<red>.");
        messages.put("service.world.weather-set", "<primary>Set the weather in <secondary>{world}<primary> to <secondary>{weather}<primary>.");
        messages.put("service.world.weather-failed", "<red>Failed to set the weather in <secondary>{world}<red>.");
        messages.put("service.world.remove-player-required", "<red>/remove requires a player as the radius origin.");
        messages.put("service.world.remove-success", "<primary>Removed <secondary>{count}<primary> entities.");
        messages.put("service.world.remove-failed", "<red>Failed to remove entities.");
        messages.put("commands.world.backup-complete", "<primary>Backup completed: <secondary>{file}<primary>.");
        messages.put("commands.world.backup-failed", "<red>Backup failed: <secondary>{reason}<red>.");
        messages.put("commands.world.backup-running", "<primary>Backup running.");
        messages.put("commands.world.backup-started", "<primary>Backup started.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.world.remove-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.world.time-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.world.time-command.error.2", "<red>时间格式错误: <secondary>{value0}<red>");
        messages.put("commands.world.weather-command.error.1", "<red>用法: <secondary>{value0}<red>");
        messages.put("commands.world.weather-command.error.2", "<red>未知天气: <secondary>{value0}<red>");
        messages.put("service.world.time-set", "<primary>已将世界 <secondary>{world}<primary> 的时间设置为 <secondary>{time}<primary>。");
        messages.put("service.world.time-failed", "<red>设置世界 <secondary>{world}<red> 的时间失败。");
        messages.put("service.world.weather-set", "<primary>已将世界 <secondary>{world}<primary> 的天气设置为 <secondary>{weather}<primary>。");
        messages.put("service.world.weather-failed", "<red>设置世界 <secondary>{world}<red> 的天气失败。");
        messages.put("service.world.remove-player-required", "<red>/remove 需要玩家作为半径中心。");
        messages.put("service.world.remove-success", "<primary>已移除 <secondary>{count}<primary> 个实体。");
        messages.put("service.world.remove-failed", "<red>实体移除失败。");
        messages.put("commands.world.backup-complete", "<primary>备份完成：<secondary>{file}<primary>。");
        messages.put("commands.world.backup-failed", "<red>备份失败：<secondary>{reason}<red>。");
        messages.put("commands.world.backup-running", "<primary>Backup running。");
        messages.put("commands.world.backup-started", "<primary>操作成功：Backup started。");
        return Map.copyOf(messages);
    }

}
