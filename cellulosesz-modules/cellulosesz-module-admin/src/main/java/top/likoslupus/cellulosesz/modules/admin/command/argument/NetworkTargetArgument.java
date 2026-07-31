package top.likoslupus.cellulosesz.modules.admin.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.modules.admin.service.IpAddresses;

import java.util.Collection;
import java.util.List;

public final class NetworkTargetArgument implements ArgumentType<NetworkTargetInput> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid IP address or player name: " + value)
    );
    private final boolean allowPlayerName;

    private NetworkTargetArgument(boolean allowPlayerName) {
        this.allowPlayerName = allowPlayerName;
    }

    public static NetworkTargetArgument addressOrPlayer() {
        return new NetworkTargetArgument(true);
    }

    public static NetworkTargetArgument addressOnly() {
        return new NetworkTargetArgument(false);
    }

    public static NetworkTargetInput get(CommandContext<?> context, String name) {
        return context.getArgument(name, NetworkTargetInput.class);
    }

    @Override
    public NetworkTargetInput parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();
        var addressLike = token.contains(":") || token.contains(".") || token.startsWith("[");
        var address = IpAddresses.parseLiteral(token);

        if (address.isPresent()) {
            return new NetworkTargetInput.Address(address.orElseThrow());
        }
        if (allowPlayerName && !addressLike && token.matches("[A-Za-z0-9_]{1,16}")) {
            return new NetworkTargetInput.PlayerName(token);
        }

        reader.setCursor(start);
        throw INVALID.createWithContext(reader, token);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "127.0.0.1",
                "::1",
                "[2001:db8::1]",
                "Alice"
        );
    }

}
