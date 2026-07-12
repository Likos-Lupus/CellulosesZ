package top.likoslupus.cellulosesz.api.command.service;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface CommandSuggestionRegistry {

    void register(
            String command,
            String argument,
            Function<CommandSuggestionContext, Collection<String>> provider
    );

    List<String> suggest(CommandSuggestionContext context);

}
