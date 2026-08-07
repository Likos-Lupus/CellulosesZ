package top.likoslupus.cellulosesz.modules.playerstate.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

public final class MovementSpeeds {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid movement speed: " + value)
    );

    private MovementSpeeds() {
    }

    public static double validate(
            double value,
            double minimum,
            double maximum
    ) throws CommandSyntaxException {
        if (!Double.isFinite(value)
                || value < minimum
                || value > maximum
        ) {
            throw INVALID.create(value);
        }

        return value;
    }

}
