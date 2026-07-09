package top.likoslupus.cellulosesz.core.i18n;

import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DefaultMessageService implements MessageService {

    private final Path directory;
    private final CellulosesZLogger logger;
    private final Map<String, String> messages = new LinkedHashMap<>();
    private String locale = "zh_cn";
    private String fallback = "en_us";

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

    private String normalizeLocale(String locale) {
        return locale.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    @Override
    public String message(String key) {
        return message(key, Map.of());
    }

    @Override
    public String message(String key, Map<String, ?> placeholders) {
        key = messages.getOrDefault(key, "<missing message: %s>".formatted(key));
        for (var entry : placeholders.entrySet()) {
            key = key.replace("{%s}".formatted(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return key;
    }

    @Override
    public void reload() {
        try {
            Files.createDirectories(directory);
            writeDefaultIfMissing("en_us", defaultEnglish());
            writeDefaultIfMissing("zh_cn", defaultChinese());

            Map<String, String> loaded = new LinkedHashMap<>();
            loaded.putAll(readFlattened(directory.resolve(fallback + ".yml")));
            loaded.putAll(readFlattened(directory.resolve(locale + ".yml")));

            messages.clear();
            messages.putAll(loaded);
        } catch (IOException exception) {
            logger.error("Failed to load messages", exception);
        }
    }

    private void writeDefaultIfMissing(String name, Map<String, Object> value) throws IOException {
        var path = directory.resolve(name + ".yml");
        if (Files.notExists(path)) {
            JacksonCodecs.writeYaml(path, value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readFlattened(Path path) throws IOException {
        if (Files.notExists(path)) return Map.of();

        Map<String, Object> raw = JacksonCodecs.yaml().readValue(path.toFile(), Map.class);
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
        for (var entry : raw.entrySet()) {
            var key = prefix.isBlank()
                    ? entry.getKey()
                    : prefix + "." + entry.getKey();
            var value = entry.getValue();

            if (value instanceof Map<?, ?> map) {
                flatten(key, (Map<String, Object>) map, flattened);
            } else {
                flattened.put(key, String.valueOf(value));
            }
        }
    }

    private Map<String, Object> defaultEnglish() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("common", Map.of(
                "no-permission", "You do not have permission to use this command.",
                "player-only", "This command can only be used by a player.",
                "console-only", "This command can only be used from the console."
        ));
        root.put("cellulosesz", Map.of(
                "version", "CellulosesZ {version}",
                "reloaded", "CellulosesZ has been reloaded.",
                "modules-header", "Loaded CellulosesZ modules:",
                "unknown-subcommand", "Unknown CellulosesZ subcommand."
        ));
        return root;
    }

    private Map<String, Object> defaultChinese() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("common", Map.of(
                "no-permission", "你没有权限执行此命令。",
                "player-only", "此命令只能由玩家执行。",
                "console-only", "此命令只能由控制台执行。"
        ));
        root.put("cellulosesz", Map.of(
                "version", "CellulosesZ {version}",
                "reloaded", "CellulosesZ 已重载。",
                "modules-header", "已加载的 CellulosesZ 模块：",
                "unknown-subcommand", "未知的 CellulosesZ 子命令。"
        ));
        return root;
    }

}
