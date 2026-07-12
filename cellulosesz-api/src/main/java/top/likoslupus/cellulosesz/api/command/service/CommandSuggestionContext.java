package top.likoslupus.cellulosesz.api.command.service;

import java.util.List;
import java.util.Optional;

public record CommandSuggestionContext(
        String command,
        String argument,
        String remaining,
        List<String> parsedArguments,
        Optional<String> playerName
) {

}
