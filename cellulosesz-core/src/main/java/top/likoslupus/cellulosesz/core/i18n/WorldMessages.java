package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class WorldMessages {

    private WorldMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.world.backup-complete", "<primary>Backup completed: <secondary>{file}<primary>.");
        messages.put("commands.world.backup-failed", "<red>Backup failed: <secondary>{reason}<red>.");
        messages.put("commands.world.backup-running", "<primary>Backup running.");
        messages.put("commands.world.backup-started", "<primary>Backup started.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.world.backup-complete", "<primary>备份完成：<secondary>{file}<primary>。");
        messages.put("commands.world.backup-failed", "<red>备份失败：<secondary>{reason}<red>。");
        messages.put("commands.world.backup-running", "<primary>Backup running。");
        messages.put("commands.world.backup-started", "<primary>操作成功：Backup started。");
        return Map.copyOf(messages);
    }

}
