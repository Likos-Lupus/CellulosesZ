package top.likoslupus.cellulosesz.modules.teleport.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestSelector;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class TeleportRequestSelectorArgument implements ArgumentType<TeleportRequestSelector> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid request UUID or player name: " + value)
    );

    public static TeleportRequestSelectorArgument selector() {
        return new TeleportRequestSelectorArgument();
    }

    public static TeleportRequestSelector get(CommandContext<?> context, String name) {
        return context.getArgument(name, TeleportRequestSelector.class);
    }

    @Override
    public TeleportRequestSelector parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();

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

        reader.setCursor(start);
        throw INVALID.createWithContext(reader, token);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "00000000-0000-0000-0000-000000000001",
                "Alice"
        );
    }

}
