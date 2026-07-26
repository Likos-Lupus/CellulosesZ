package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class AdminMessages {

    private AdminMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.admin.ban-ip.unknown-address", "<red>Unknown address.");
        messages.put("commands.admin.ban-ip.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.admin.maximum-punishment", "<primary>Maximum punishment.");
        messages.put("commands.admin.temp-ban-ip.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.admin.unban-ip.not-found", "<red>Not found.");
        messages.put("commands.admin.unban-ip.success", "<primary>Success.");
        messages.put("commands.admin.unban-ip.usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.admin.unban.not-found", "<red>Not found.");
        messages.put("commands.admin.unban.success", "<primary>Success.");
        messages.put("commands.admin.unban.usage", "<red>Usage: <secondary>{usage}");
        messages.put("service.admin.invalid-address", "<red>Invalid address.");
        messages.put("service.admin.invalid-duration", "<red>Invalid duration.");
        messages.put("service.admin.not-muted", "<primary>Not muted.");
        messages.put("service.admin.temp-ban-ip-not-found", "<red>Temp ban ip not found.");
        messages.put("service.admin.temp-ban-not-found", "<red>Temp ban not found.");
        messages.put("service.admin.temp-unban-ip-success", "<primary>Temp unban ip success.");
        messages.put("service.admin.temp-unban-success", "<primary>Temp unban success.");
        messages.put("service.admin.jail-invalid-name", "<red>The jail name is invalid.");
        messages.put("service.admin.kick-all-partial", "<primary>Kickall completed partially: <secondary>{kicked}<primary> kicked, <secondary>{exempt}<primary> exempt, <secondary>{failed}<primary> failed.");
        messages.put("service.admin.kick-all-failed", "<red>Kickall failed: <secondary>{kicked}<red> kicked, <secondary>{exempt}<red> exempt, <secondary>{failed}<red> failed.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.admin.ban-ip.unknown-address", "<red>操作失败：Unknown address。");
        messages.put("commands.admin.ban-ip.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.admin.maximum-punishment", "<primary>Maximum punishment。");
        messages.put("commands.admin.temp-ban-ip.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.admin.unban-ip.not-found", "<red>操作失败：Not found。");
        messages.put("commands.admin.unban-ip.success", "<primary>操作成功：Success。");
        messages.put("commands.admin.unban-ip.usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.admin.unban.not-found", "<red>操作失败：Not found。");
        messages.put("commands.admin.unban.success", "<primary>操作成功：Success。");
        messages.put("commands.admin.unban.usage", "<red>用法：<secondary>{usage}");
        messages.put("service.admin.invalid-address", "<red>操作失败：Invalid address。");
        messages.put("service.admin.invalid-duration", "<red>操作失败：Invalid duration。");
        messages.put("service.admin.not-muted", "<primary>Not muted。");
        messages.put("service.admin.temp-ban-ip-not-found", "<red>操作失败：Temp ban ip not found。");
        messages.put("service.admin.temp-ban-not-found", "<red>操作失败：Temp ban not found。");
        messages.put("service.admin.temp-unban-ip-success", "<primary>操作成功：Temp unban ip success。");
        messages.put("service.admin.temp-unban-success", "<primary>操作成功：Temp unban success。");
        messages.put("service.admin.jail-invalid-name", "<red>监狱名称无效。");
        messages.put("service.admin.kick-all-partial", "<primary>批量踢出部分完成：已踢出 <secondary>{kicked}<primary>，豁免 <secondary>{exempt}<primary>，失败 <secondary>{failed}<primary>。");
        messages.put("service.admin.kick-all-failed", "<red>批量踢出失败：已踢出 <secondary>{kicked}<red>，豁免 <secondary>{exempt}<red>，失败 <secondary>{failed}<red>。");
        return Map.copyOf(messages);
    }

}
