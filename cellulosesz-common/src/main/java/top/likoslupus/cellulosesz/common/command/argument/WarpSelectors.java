package top.likoslupus.cellulosesz.common.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class WarpSelectors {

    private static final SimpleCommandExceptionType NUMERIC = new SimpleCommandExceptionType(
            new LiteralMessage("A numeric warp selector is reserved for page numbers")
    );

    private WarpSelectors() {
    }

    public static Selection parse(String value) throws CommandSyntaxException {
        if (value.matches("[+-]?\\d+")) {
            if (value.startsWith("+")) {
                throw NUMERIC.create();
            }

            try {
                var page = Integer.parseInt(value);
                if (page > 0) {
                    return new Selection.Page(page);
                }
            } catch (NumberFormatException _) {
            }

            throw NUMERIC.create();
        }

        return new Selection.Name(value);
    }

    public sealed interface Selection permits Selection.Name, Selection.Page {

        record Name(
                String value
        ) implements Selection {

            public Name {
                requireNonNull(value, "value");
            }

        }

        record Page(
                int value
        ) implements Selection {

            public Page {
                requirePositive(value, "value");
            }

        }

    }

}
