package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.command.service.CommandAliasRegistry;

import java.util.*;

public final class DefaultCommandAliasRegistry implements CommandAliasRegistry {

    private final Map<String, LinkedHashSet<String>> declared = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<String>> configured = new LinkedHashMap<>();

    @Override
    public synchronized void register(String command, Collection<String> values) {
        add(declared, command, values);
    }

    @Override
    public synchronized List<String> aliases(String command) {
        var normalized = normalize(command);
        var result = new LinkedHashSet<String>();
        result.addAll(declared.getOrDefault(normalized, new LinkedHashSet<>()));
        result.addAll(configured.getOrDefault(normalized, new LinkedHashSet<>()));
        result.remove(normalized);
        return List.copyOf(result);
    }

    private void add(
            Map<String, LinkedHashSet<String>> targetMap,
            String command,
            Collection<String> values
    ) {
        if (command.isBlank()) return;
        var normalized = normalize(command);
        var target = targetMap.computeIfAbsent(normalized, _ -> new LinkedHashSet<>());
        values.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank() && !value.equals(normalized))
                .forEach(target::add);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public synchronized void configure(Map<String, ? extends Collection<String>> values) {
        configured.clear();
        values.forEach(
                (command, aliases) -> add(configured, command, aliases)
        );
    }

}
