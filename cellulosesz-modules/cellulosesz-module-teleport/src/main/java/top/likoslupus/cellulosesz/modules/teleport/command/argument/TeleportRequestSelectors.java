package top.likoslupus.cellulosesz.modules.teleport.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequestSelector;

import java.util.UUID;

public final class TeleportRequestSelectors {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid request UUID or player name: " + value)
    );

    private TeleportRequestSelectors() {
    }

    public static TeleportRequestSelector parse(String token) throws CommandSyntaxException {
        try {
            var id = UUID.fromString(token);
            if (id.toString().equalsIgnoreCase(token)) {
                return new TeleportRequestSelector.RequestId(id);
            }
        } catch (IllegalArgumentException _) {
        }

        if (token.matches("[A-Za-z0-9_]{1,16}")) {
            return new TeleportRequestSelector.PlayerName(token);
        }

        throw INVALID.create(token);
    }

}
