package top.likoslupus.cellulosesz.api.command.spec;

import java.util.List;

public record CommandParameter(
        String name,
        CommandParameterType type,
        boolean optional,
        List<String> choices
) {

    public CommandParameter {
        choices = List.copyOf(choices);
    }

    public static CommandParameter required(String name, CommandParameterType type) {
        return new CommandParameter(name, type, false, List.of());
    }

    public static CommandParameter optional(String name, CommandParameterType type) {
        return new CommandParameter(name, type, true, List.of());
    }

    public static CommandParameter choice(
            String name,
            boolean optional,
            String... values
    ) {
        return new CommandParameter(name, CommandParameterType.WORD, optional, List.of(values));
    }

}
