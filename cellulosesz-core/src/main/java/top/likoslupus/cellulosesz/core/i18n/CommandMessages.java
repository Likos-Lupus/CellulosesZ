package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class CommandMessages {

    private CommandMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.command.help-empty", "<red>No commands matched <secondary>{query}<red>.");
        messages.put("commands.command.help-entry", "<primary>/<secondary>{command}");
        messages.put("commands.command.help-header", "<primary>Help page <secondary>{page}<primary>/<secondary>{pages}<primary> — <secondary>{query}");
        messages.put("commands.command.help-usage", "<red>Usage: <secondary>{usage}<red>");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.command.help-empty", "<red>没有命令匹配 <secondary>{query}<red>。");
        messages.put("commands.command.help-entry", "<primary>/<secondary>{command}");
        messages.put("commands.command.help-header", "<primary>帮助第 <secondary>{page}<primary>/<secondary>{pages}<primary> 页——<secondary>{query}");
        messages.put("commands.command.help-usage", "<red>用法：<secondary>{usage}<red>");
        return Map.copyOf(messages);
    }

}
