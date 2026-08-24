package top.likoslupus.cellulosesz.core.command.service;

import java.util.*;

public final class DefaultCommandAliasRegistry implements CommandAliasRegistry {

    private final Map<String, LinkedHashSet<String>> declared = new LinkedHashMap<>();
    private volatile Map<String, Set<String>> configured = Map.of();

    @Override
    public synchronized void register(String command, Collection<String> values) {
        add(declared, command, values);
    }

    @Override
    public synchronized List<String> aliases(String command) {
        var normalized = normalize(command);
        var result = new LinkedHashSet<String>();

        result.addAll(declared.getOrDefault(normalized, new LinkedHashSet<>()));
        result.addAll(configured.getOrDefault(normalized, Set.of()));
        result.remove(normalized);

        return List.copyOf(result);
    }

    private void add(
            Map<String, LinkedHashSet<String>> targetMap,
            String command,
            Collection<String> values
    ) {
        if (command.isBlank()) {
            return;
        }

        var normalized = normalize(command);
        var target = targetMap.computeIfAbsent(
                normalized,
                _ -> new LinkedHashSet<>()
        );

        values.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank()
                        && !value.equals(normalized)
                )
                .forEach(target::add);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public void configure(Map<String, ? extends Collection<String>> values) {
        var next = new LinkedHashMap<String, LinkedHashSet<String>>();
        values.forEach((command, aliases) ->
                add(next, command, aliases)
        );

        var immutable = new LinkedHashMap<String, Set<String>>();
        next.forEach((command, aliases) ->
                immutable.put(command, Set.copyOf(aliases))
        );
        configured = Map.copyOf(immutable);
    }

}
