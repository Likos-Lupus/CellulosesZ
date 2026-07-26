package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class TextMessages {

    private TextMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.text.custom-missing", "<primary>Custom missing.");
        messages.put("commands.text.custom-title", "<primary>{name} — <secondary>{page}<primary>/<secondary>{pages}");
        messages.put("commands.text.custom-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.text.empty", "<red>Empty.");
        messages.put("commands.text.info-title", "<primary>Info — <secondary>{page}<primary>/<secondary>{pages}");
        messages.put("commands.text.line", "{line}");
        messages.put("commands.text.motd-title", "<primary>MOTD — <secondary>{page}<primary>/<secondary>{pages}");
        messages.put("commands.text.rules-title", "<primary>Rules — <secondary>{page}<primary>/<secondary>{pages}");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.text.custom-missing", "<primary>Custom missing。");
        messages.put("commands.text.custom-title", "<primary>{name}——<secondary>{page}<primary>/<secondary>{pages}");
        messages.put("commands.text.custom-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.text.empty", "<red>操作失败：Empty。");
        messages.put("commands.text.info-title", "<primary>信息——<secondary>{page}<primary>/<secondary>{pages}");
        messages.put("commands.text.line", "{line}");
        messages.put("commands.text.motd-title", "<primary>每日消息——<secondary>{page}<primary>/<secondary>{pages}");
        messages.put("commands.text.rules-title", "<primary>规则——<secondary>{page}<primary>/<secondary>{pages}");
        return Map.copyOf(messages);
    }

}
