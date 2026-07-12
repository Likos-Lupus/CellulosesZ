package top.likoslupus.cellulosesz.api.command.spec;

import java.util.List;

public record CommandSpec(List<CommandRoute> routes) {

    public CommandSpec {
        routes = List.copyOf(routes);
    }

    public static CommandSpec auto() {
        return new CommandSpec(List.of());
    }

    public static CommandSpec of(CommandRoute... routes) {
        return new CommandSpec(List.of(routes));
    }

    public boolean automatic() {
        return routes.isEmpty();
    }

}
