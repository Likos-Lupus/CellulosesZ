package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class CommandMessages {

    private CommandMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.command.help-command.reply.1", "<primary>CellulosesZ commands:");
        messages.put("commands.command.help-command.reply.2", "<primary> /<secondary>{value0}<primary> - <secondary>{value1}<primary>");
        messages.put("commands.command.root-celluloses-z-command.reply.1", "<primary>Usage: <secondary>{value0}<primary>");
        messages.put("commands.command.root-celluloses-z-command.reply.2", "<primary>CellulosesZ debug: version=<secondary>{value0}<primary>, modules=<secondary>{value1}<primary>");
        messages.put("cellulosesz.module-row", "<secondary>{module}");
        messages.put("cellulosesz.reload-failed", "<red>Reload failed; the previous configuration remains active: <secondary>{reason}");
        messages.put("commands.command.help-empty", "<red>No commands matched <secondary>{query}<red>.");
        messages.put("commands.command.help-entry", "<primary>/<secondary>{command}");
        messages.put("commands.command.help-header", "<primary>Help page <secondary>{page}<primary>/<secondary>{pages}<primary> — <secondary>{query}");
        messages.put("commands.command.help-usage", "<red>Usage: <secondary>{usage}<red>");
        messages.put("cellulosesz.version", "<primary>CellulosesZ <secondary>{version}");
        messages.put("cellulosesz.reload-started", "<secondary>CellulosesZ reload started.");
        messages.put("cellulosesz.reloaded", "<primary>CellulosesZ has been reloaded.");
        messages.put("cellulosesz.modules-header", "<primary>Loaded CellulosesZ modules:");
        messages.put("cellulosesz.unknown-subcommand", "<red>Unknown CellulosesZ subcommand.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.command.help-command.reply.1", "<primary>CellulosesZ commands:");
        messages.put("commands.command.help-command.reply.2", "<primary> /<secondary>{value0}<primary> - <secondary>{value1}<primary>");
        messages.put("commands.command.root-celluloses-z-command.reply.1", "<primary>Usage: <secondary>{value0}<primary>");
        messages.put("commands.command.root-celluloses-z-command.reply.2", "<primary>CellulosesZ debug: version=<secondary>{value0}<primary>, modules=<secondary>{value1}<primary>");
        messages.put("cellulosesz.module-row", "<secondary>{module}");
        messages.put("cellulosesz.reload-failed", "<red>重载失败，旧配置仍然有效：<secondary>{reason}");
        messages.put("commands.command.help-empty", "<red>没有命令匹配 <secondary>{query}<red>。");
        messages.put("commands.command.help-entry", "<primary>/<secondary>{command}");
        messages.put("commands.command.help-header", "<primary>帮助第 <secondary>{page}<primary>/<secondary>{pages}<primary> 页——<secondary>{query}");
        messages.put("commands.command.help-usage", "<red>用法：<secondary>{usage}<red>");
        messages.put("cellulosesz.version", "<primary>CellulosesZ <secondary>{version}");
        messages.put("cellulosesz.reload-started", "<secondary>CellulosesZ 重载已开始。");
        messages.put("cellulosesz.reloaded", "<primary>CellulosesZ 已重载。");
        messages.put("cellulosesz.modules-header", "<primary>已加载的 CellulosesZ 模块：");
        messages.put("cellulosesz.unknown-subcommand", "<red>未知的 CellulosesZ 子命令。");
        return Map.copyOf(messages);
    }

}
