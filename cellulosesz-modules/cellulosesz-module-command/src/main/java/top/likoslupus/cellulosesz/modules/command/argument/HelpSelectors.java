package top.likoslupus.cellulosesz.modules.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import static java.util.Objects.requireNonNull;

public final class HelpSelectors {

    private static final SimpleCommandExceptionType INTEGER_QUERY = new SimpleCommandExceptionType(
            new LiteralMessage("An integer help token must be a positive page number")
    );

    private HelpSelectors() {
    }

    public static Selection parse(String value) throws CommandSyntaxException {
        if (!value.matches("[+-]?\\d+")) {
            return new Selection.Query(value);
        }

        if (value.startsWith("+")) {
            throw INTEGER_QUERY.create();
        }

        try {
            var page = Integer.parseInt(value);
            if (page <= 0) {
                throw INTEGER_QUERY.create();
            }

            return new Selection.Page(page);
        } catch (NumberFormatException ignored) {
            throw INTEGER_QUERY.create();
        }
    }

    public static String requireQuery(Selection selection) throws CommandSyntaxException {
        return switch (selection) {
            case Selection.Query(var value) -> value;
            case Selection.Page _ -> throw INTEGER_QUERY.create();
        };
    }

    public sealed interface Selection permits Selection.Page, Selection.Query {

        record Page(int value) implements Selection {

            public Page {
                if (value <= 0) {
                    throw new IllegalArgumentException("value must be positive");
                }
            }

        }

        record Query(String value) implements Selection {

            public Query {
                value = requireNonNull(value, "value");
            }

        }

    }

}
