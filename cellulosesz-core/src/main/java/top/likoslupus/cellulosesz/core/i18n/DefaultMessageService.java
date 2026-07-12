package top.likoslupus.cellulosesz.core.i18n;

import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.text.TextStyle;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class DefaultMessageService implements MessageService, MessageRenderer {

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Map<Character, String> LEGACY_COLORS = Map.ofEntries(
            Map.entry('0', "#000000"),
            Map.entry('1', "#0000AA"),
            Map.entry('2', "#00AA00"),
            Map.entry('3', "#00AAAA"),
            Map.entry('4', "#AA0000"),
            Map.entry('5', "#AA00AA"),
            Map.entry('6', "#FFAA00"),
            Map.entry('7', "#AAAAAA"),
            Map.entry('8', "#555555"),
            Map.entry('9', "#5555FF"),
            Map.entry('a', "#55FF55"),
            Map.entry('b', "#55FFFF"),
            Map.entry('c', "#FF5555"),
            Map.entry('d', "#FF55FF"),
            Map.entry('e', "#FFFF55"),
            Map.entry('f', "#FFFFFF")
    );
    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", "#000000"),
            Map.entry("dark_blue", "#0000AA"),
            Map.entry("dark_green", "#00AA00"),
            Map.entry("dark_aqua", "#00AAAA"),
            Map.entry("dark_red", "#AA0000"),
            Map.entry("dark_purple", "#AA00AA"),
            Map.entry("gold", "#FFAA00"),
            Map.entry("gray", "#AAAAAA"),
            Map.entry("dark_gray", "#555555"),
            Map.entry("blue", "#5555FF"),
            Map.entry("green", "#55FF55"),
            Map.entry("aqua", "#55FFFF"),
            Map.entry("red", "#FF5555"),
            Map.entry("light_purple", "#FF55FF"),
            Map.entry("yellow", "#FFFF55"),
            Map.entry("white", "#FFFFFF")
    );

    private final Path directory;
    private final CellulosesZLogger logger;
    private final Map<String, Map<String, String>> locales = new LinkedHashMap<>();
    private String locale = "zh_cn";
    private String fallback = "en_us";
    private String primaryColor = "#55FF55";
    private String secondaryColor = "#FFFF55";
    private boolean legacyColors = true;

    public DefaultMessageService(
            Path directory,
            CellulosesZLogger logger
    ) {
        this.directory = directory;
        this.logger = logger;
    }

    public void locales(String locale, String fallback) {
        this.locale = normalizeLocale(locale);
        this.fallback = normalizeLocale(fallback);
    }

    private String normalizeLocale(String value) {
        if (value.isBlank()) return locale;
        return value
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
    }

    public void theme(
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {
        this.primaryColor = normalizeColor(primaryColor, "#55FF55");
        this.secondaryColor = normalizeColor(secondaryColor, "#FFFF55");
        this.legacyColors = legacyColors;
    }

    private String normalizeColor(String value, String fallbackColor) {
        var normalized = value.trim();
        if (!normalized.startsWith("#")) normalized = "#" + normalized;
        return HEX.matcher(normalized).matches()
                ? normalized.toUpperCase(Locale.ROOT)
                : fallbackColor;
    }

    @Override
    public String message(String key) {
        return message(key, Map.of());
    }

    @Override
    public String message(String key, Map<String, ?> placeholders) {
        return rich(locale, key, placeholders).plainText();
    }

    @Override
    public RichText rich(
            String locale,
            String key,
            Map<String, ?> placeholders
    ) {
        return render(locale, key, placeholders);
    }

    @Override
    public boolean contains(String locale, String key) {
        var normalized = normalizeLocale(locale);
        return messages(normalized).containsKey(key) || messages(fallback).containsKey(key);
    }

    @Override
    public synchronized void reload() {
        try {
            Files.createDirectories(directory);
            writeDefaultIfMissing("en_us", defaultEnglish());
            writeDefaultIfMissing("zh_cn", defaultChinese());

            locales.clear();
            loadLocale(fallback);
            loadLocale(locale);
        } catch (IOException exception) {
            logger.error("Failed to load messages", exception);
        }
    }

    @Override
    public RichText render(
            String requestedLocale,
            String key,
            Map<String, ?> placeholders
    ) {
        var template = lookup(normalizeLocale(requestedLocale), key)
                .orElse("<red><missing message: " + key + ">");
        return renderInline(requestedLocale, template, placeholders);
    }

    @Override
    public RichText renderInline(
            String requestedLocale,
            String template,
            Map<String, ?> placeholders
    ) {
        return parse(normalizeLocale(requestedLocale), template, placeholders);
    }

    private Optional<String> lookup(String requestedLocale, String key) {
        var requested = messages(requestedLocale).get(key);
        if (requested != null) return Optional.of(requested);

        var configured = messages(locale).get(key);
        if (configured != null) return Optional.of(configured);

        return Optional.ofNullable(messages(fallback).get(key));
    }

    private synchronized Map<String, String> messages(String requestedLocale) {
        var normalized = normalizeLocale(requestedLocale);
        var current = locales.get(normalized);
        if (current != null) return current;

        try {
            return loadLocale(normalized);
        } catch (IOException exception) {
            logger.warn("Failed to load locale " + normalized + ": " + exception.getMessage());
            locales.put(normalized, Map.of());
            return Map.of();
        }
    }

    private Map<String, String> loadLocale(String name) throws IOException {
        var loaded = new LinkedHashMap<String, String>();
        if (name.equals("en_us")) flatten("", defaultEnglish(), loaded);
        if (name.equals("zh_cn")) flatten("", defaultChinese(), loaded);

        loaded.putAll(readFlattened(directory.resolve(name + ".yml")));
        var immutable = Map.copyOf(loaded);
        locales.put(name, immutable);
        return immutable;
    }

    private RichText parse(String requestedLocale, String input, Map<String, ?> placeholders) {
        var segments = new ArrayList<RichText.Segment>();
        var buffer = new StringBuilder();
        var style = TextStyle.EMPTY;

        for (var index = 0; index < input.length(); ) {
            var current = input.charAt(index);
            if (current == '{') {
                var end = input.indexOf('}', index + 1);
                if (end > index) {
                    var key = input.substring(index + 1, end);
                    if (placeholders.containsKey(key)) {
                        var value = placeholders.get(key);
                        if (value instanceof RichText(List<RichText.Segment> segments1)) {
                            flush(segments, buffer, style);
                            segments.addAll(segments1);
                        } else if (value instanceof LocalizedMessage(String key1, Map<String, Object> placeholders1)) {
                            flush(segments, buffer, style);
                            segments.addAll(render(
                                    requestedLocale,
                                    key1,
                                    placeholders1
                            ).segments());
                        } else {
                            buffer.append(value);
                        }
                        index = end + 1;
                        continue;
                    }
                }
            }
            if (current == '<') {
                var end = input.indexOf('>', index + 1);
                if (end > index) {
                    var tag = input.substring(index + 1, end).trim().toLowerCase(Locale.ROOT);
                    var updated = applyTag(style, tag);
                    if (updated.isPresent()) {
                        flush(segments, buffer, style);
                        style = updated.orElseThrow();
                        index = end + 1;
                        continue;
                    }
                }
            }

            if (legacyColors && (current == '&' || current == '§') && index + 1 < input.length()) {
                if (input.charAt(index + 1) == '#' && index + 8 <= input.length()) {
                    var hex = input.substring(index + 1, index + 8);
                    if (HEX.matcher(hex).matches()) {
                        flush(segments, buffer, style);
                        style = style.withColor(hex.toUpperCase(Locale.ROOT));
                        index += 8;
                        continue;
                    }
                }
                var code = Character.toLowerCase(input.charAt(index + 1));
                var updated = applyLegacy(style, code);
                if (updated.isPresent()) {
                    flush(segments, buffer, style);
                    style = updated.orElseThrow();
                    index += 2;
                    continue;
                }
            }

            buffer.append(current);
            index++;
        }
        flush(segments, buffer, style);
        return new RichText(segments);
    }

    private Optional<TextStyle> applyTag(TextStyle style, String tag) {
        return switch (tag) {
            case "primary" -> Optional.of(style.withColor(primaryColor));
            case "secondary" -> Optional.of(style.withColor(secondaryColor));
            case "bold", "b" -> Optional.of(style.withBold(true));
            case "/bold", "/b" -> Optional.of(style.withBold(false));
            case "italic", "i" -> Optional.of(style.withItalic(true));
            case "/italic", "/i" -> Optional.of(style.withItalic(false));
            case "underlined", "underline", "u" -> Optional.of(style.withUnderlined(true));
            case "/underlined", "/underline", "/u" -> Optional.of(style.withUnderlined(false));
            case "strikethrough", "st" -> Optional.of(style.withStrikethrough(true));
            case "/strikethrough", "/st" -> Optional.of(style.withStrikethrough(false));
            case "obfuscated", "magic" -> Optional.of(style.withObfuscated(true));
            case "/obfuscated", "/magic" -> Optional.of(style.withObfuscated(false));
            case "reset", "/reset" -> Optional.of(TextStyle.EMPTY);
            default -> {
                if (HEX.matcher(tag).matches()) {
                    yield Optional.of(style.withColor(tag.toUpperCase(Locale.ROOT)));
                }
                yield Optional.ofNullable(NAMED_COLORS.get(tag))
                        .map(style::withColor);
            }
        };
    }

    private Optional<TextStyle> applyLegacy(TextStyle style, char code) {
        var color = LEGACY_COLORS.get(code);
        if (color != null) return Optional.of(TextStyle.EMPTY.withColor(color));

        return switch (code) {
            case 'k' -> Optional.of(style.withObfuscated(true));
            case 'l' -> Optional.of(style.withBold(true));
            case 'm' -> Optional.of(style.withStrikethrough(true));
            case 'n' -> Optional.of(style.withUnderlined(true));
            case 'o' -> Optional.of(style.withItalic(true));
            case 'r' -> Optional.of(TextStyle.EMPTY);
            default -> Optional.empty();
        };
    }

    private void flush(
            List<RichText.Segment> segments,
            StringBuilder buffer,
            TextStyle style
    ) {
        if (buffer.isEmpty()) return;
        segments.add(new RichText.Segment(buffer.toString(), style));
        buffer.setLength(0);
    }

    private void writeDefaultIfMissing(String name, Map<String, Object> value) throws IOException {
        var path = directory.resolve(name + ".yml");
        if (Files.notExists(path)) JacksonCodecs.writeYaml(path, value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readFlattened(Path path) throws IOException {
        if (Files.notExists(path)) return Map.of();

        Map<String, Object> raw = JacksonCodecs.readYaml(path, Map.class);
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten("", raw, flattened);
        return flattened;
    }

    @SuppressWarnings("unchecked")
    private void flatten(
            String prefix,
            Map<String, Object> raw,
            Map<String, String> flattened
    ) {
        raw.forEach((key1, value) -> {
            var key = prefix.isBlank()
                    ? key1
                    : prefix + "." + key1;

            if (value instanceof Map<?, ?> map) {
                flatten(key, (Map<String, Object>) map, flattened);
            } else if (!key.equals("schema")) {
                flattened.put(key, String.valueOf(value));
            }
        });
    }

    private Map<String, Object> defaultEnglish() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("common", Map.ofEntries(
                Map.entry("no-permission", "<red>You do not have permission to use this command."),
                Map.entry("player-only", "<red>This command can only be used by a player."),
                Map.entry("console-only", "<red>This command can only be used from the console."),
                Map.entry("usage", "<red>Usage: <secondary>{usage}"),
                Map.entry("player-not-found", "<red>Player not found: <secondary>{player}"),
                Map.entry("module-disabled", "<red>Module is disabled: <secondary>{module}"),
                Map.entry("command-cost-failed", "<red>You need <secondary>{cost}<red> to use this command.")
        ));
        root.put("cellulosesz", Map.of(
                "version", "<primary>CellulosesZ <secondary>{version}",
                "reloaded", "<primary>CellulosesZ has been reloaded.",
                "modules-header", "<primary>Loaded CellulosesZ modules:",
                "unknown-subcommand", "<red>Unknown CellulosesZ subcommand."
        ));
        root.put("player", Map.of(
                "list", "<primary>Online players (<secondary>{count}<primary>): {players}",
                "nick-set", "<primary>Nickname set to <secondary>{nickname}<primary>.",
                "nick-cleared", "<primary>Nickname cleared.",
                "nick-invalid", "<red>The nickname does not match the configured rules."
        ));
        root.put("messaging", Map.ofEntries(
                Map.entry("private-incoming", "<dark_gray>[Message] <secondary>{sender} <dark_gray>→ <primary>you<dark_gray>: <white>{message}"),
                Map.entry("private-outgoing", "<dark_gray>[Message] <primary>you <dark_gray>→ <secondary>{target}<dark_gray>: <white>{message}"),
                Map.entry("social-spy", "<dark_gray>[SocialSpy] <secondary>{sender} <dark_gray>→ <secondary>{target}<dark_gray>: <white>{message}"),
                Map.entry("broadcast", "<gold>[Broadcast] <white>{message}"),
                Map.entry("broadcast-sent", "<primary>Broadcast sent."),
                Map.entry("me", "<secondary>* {player} <white>{action}"),
                Map.entry("helpop", "<dark_aqua>[HelpOp] <secondary>{sender}<dark_gray>: <white>{message}"),
                Map.entry("helpop-sent", "<primary>HelpOp message sent."),
                Map.entry("mail-received", "<primary>You received new mail. Use <secondary>/mail read<primary> to view it.")
        ));
        root.putAll(BuiltInCommandMessages.english());
        return root;
    }

    private Map<String, Object> defaultChinese() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("common", Map.ofEntries(
                Map.entry("no-permission", "<red>你没有权限执行此命令。"),
                Map.entry("player-only", "<red>此命令只能由玩家执行。"),
                Map.entry("console-only", "<red>此命令只能由控制台执行。"),
                Map.entry("usage", "<red>用法：<secondary>{usage}"),
                Map.entry("player-not-found", "<red>找不到玩家：<secondary>{player}"),
                Map.entry("module-disabled", "<red>模块已禁用：<secondary>{module}"),
                Map.entry("command-cost-failed", "<red>执行此命令需要 <secondary>{cost}<red>。")
        ));
        root.put("cellulosesz", Map.of(
                "version", "<primary>CellulosesZ <secondary>{version}",
                "reloaded", "<primary>CellulosesZ 已重载。",
                "modules-header", "<primary>已加载的 CellulosesZ 模块：",
                "unknown-subcommand", "<red>未知的 CellulosesZ 子命令。"
        ));
        root.put("player", Map.of(
                "list", "<primary>在线玩家（<secondary>{count}<primary>）：{players}",
                "nick-set", "<primary>昵称已设置为 <secondary>{nickname}<primary>。",
                "nick-cleared", "<primary>昵称已清除。",
                "nick-invalid", "<red>昵称不符合当前配置规则。"
        ));
        root.put("messaging", Map.ofEntries(
                Map.entry("private-incoming", "<dark_gray>[私聊] <secondary>{sender} <dark_gray>→ <primary>你<dark_gray>: <white>{message}"),
                Map.entry("private-outgoing", "<dark_gray>[私聊] <primary>你 <dark_gray>→ <secondary>{target}<dark_gray>: <white>{message}"),
                Map.entry("social-spy", "<dark_gray>[SocialSpy] <secondary>{sender} <dark_gray>→ <secondary>{target}<dark_gray>: <white>{message}"),
                Map.entry("broadcast", "<gold>[公告] <white>{message}"),
                Map.entry("broadcast-sent", "<primary>公告已发送。"),
                Map.entry("me", "<secondary>* {player} <white>{action}"),
                Map.entry("helpop", "<dark_aqua>[HelpOp] <secondary>{sender}<dark_gray>: <white>{message}"),
                Map.entry("helpop-sent", "<primary>HelpOp 消息已发送。"),
                Map.entry("mail-received", "<primary>你收到了一封新邮件，使用 <secondary>/mail read<primary> 查看。")
        ));
        root.putAll(BuiltInCommandMessages.chinese());
        return root;
    }

}
