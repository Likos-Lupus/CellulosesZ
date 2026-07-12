package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionContext;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionRegistry;

import java.util.*;
import java.util.function.Function;

public final class DefaultCommandSuggestionRegistry implements CommandSuggestionRegistry {

    private final Map<Key, Function<CommandSuggestionContext, Collection<String>>> providers = new LinkedHashMap<>();

    @Override
    public synchronized void register(
            String command,
            String argument,
            Function<CommandSuggestionContext, Collection<String>> provider
    ) {
        providers.put(new Key(normalize(command), normalize(argument)), Objects.requireNonNull(provider));
    }

    @Override
    public synchronized List<String> suggest(CommandSuggestionContext context) {
        var provider = providers.get(new Key(normalize(context.command()), normalize(context.argument())));
        if (provider == null) return List.of();

        var prefix = context.remaining().toLowerCase(Locale.ROOT);
        return provider.apply(context).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record Key(
            String command,
            String argument
    ) {

    }

}
