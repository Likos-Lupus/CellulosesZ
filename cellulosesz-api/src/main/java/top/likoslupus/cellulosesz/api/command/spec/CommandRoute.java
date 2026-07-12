package top.likoslupus.cellulosesz.api.command.spec;

import java.util.List;

public record CommandRoute(List<CommandParameter> parameters) {

    public CommandRoute {
        parameters = List.copyOf(parameters);
    }

    public static CommandRoute of(CommandParameter... parameters) {
        return new CommandRoute(List.of(parameters));
    }

}
