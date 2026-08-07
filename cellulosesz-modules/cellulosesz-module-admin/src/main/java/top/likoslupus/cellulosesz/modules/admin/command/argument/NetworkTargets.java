package top.likoslupus.cellulosesz.modules.admin.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.modules.admin.service.IpAddresses;

public final class NetworkTargets {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid IP address or player name: " + value)
    );

    private NetworkTargets() {
    }

    public static NetworkTargetInput addressOrPlayer(
            String token
    ) throws CommandSyntaxException {
        return parse(token, true);
    }

    private static NetworkTargetInput parse(
            String token,
            boolean allowPlayerName
    ) throws CommandSyntaxException {
        var addressLike = token.contains(":") || token.contains(".") || token.startsWith("[");
        var address = IpAddresses.parseLiteral(token);

        if (address.isPresent()) {
            return new NetworkTargetInput.Address(address.orElseThrow());
        }

        if (allowPlayerName
                && !addressLike
                && token.matches("[A-Za-z0-9_]{1,16}")
        ) {
            return new NetworkTargetInput.PlayerName(token);
        }

        throw INVALID.create(token);
    }

    public static NetworkTargetInput.Address addressOnly(
            String token
    ) throws CommandSyntaxException {
        var target = parse(token, false);
        if (target instanceof NetworkTargetInput.Address address) {
            return address;
        }

        throw INVALID.create(token);
    }

}
