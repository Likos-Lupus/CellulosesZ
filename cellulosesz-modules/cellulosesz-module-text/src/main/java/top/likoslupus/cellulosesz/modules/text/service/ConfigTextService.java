package top.likoslupus.cellulosesz.modules.text.service;

import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.modules.text.config.TextConfig;

import java.util.*;

public final class ConfigTextService implements TextService {

    private volatile Snapshot snapshot;

    public ConfigTextService(TextConfig config) {
        configure(config);
    }

    public void configure(TextConfig candidate) {
        snapshot = Snapshot.from(candidate);
    }

    private static List<String> validateLines(List<String> lines, String name) {
        Objects.requireNonNull(lines, name);
        return List.copyOf(lines.stream()
                .map(line -> Objects.requireNonNull(line, name + " line"))
                .toList());
    }

    @Override
    public List<String> info() {
        return snapshot.info();
    }

    @Override
    public List<String> motd() {
        return snapshot.motd();
    }

    @Override
    public List<String> rules() {
        return snapshot.rules();
    }

    @Override
    public List<String> custom(String name) {
        return snapshot.custom().getOrDefault(normalize(name), List.of());
    }

    @Override
    public Set<String> customNames() {
        return snapshot.custom().keySet();
    }

    @Override
    public int pageSize() {
        return snapshot.pageSize();
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "name").trim().toLowerCase(Locale.ROOT);
    }

    private record Snapshot(
            int pageSize,
            List<String> info,
            List<String> motd,
            List<String> rules,
            Map<String, List<String>> custom
    ) {

        private Snapshot {
            info = List.copyOf(info);
            motd = List.copyOf(motd);
            rules = List.copyOf(rules);
            custom = Map.copyOf(custom);
        }

        private static Snapshot from(TextConfig source) {
            Objects.requireNonNull(source, "config");
            if (source.pageSize < 1 || source.pageSize > 100) {
                throw new IllegalArgumentException("text.pageSize must be between 1 and 100");
            }
            var info = validateLines(source.info, "info");
            var motd = validateLines(source.motd, "motd");
            var rules = validateLines(source.rules, "rules");
            Objects.requireNonNull(source.custom, "custom");
            Map<String, List<String>> normalized = new TreeMap<>();
            source.custom.forEach((name, lines) -> {
                var key = normalize(name);
                if (key.isBlank()) throw new IllegalArgumentException("custom text name must not be blank");
                if (normalized.put(key, validateLines(lines, "custom." + key)) != null) {
                    throw new IllegalArgumentException("duplicate custom text name: " + key);
                }
            });
            return new Snapshot(source.pageSize, info, motd, rules, normalized);
        }

    }

}
