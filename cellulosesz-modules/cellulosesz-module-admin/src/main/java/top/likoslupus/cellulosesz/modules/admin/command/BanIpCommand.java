package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.AddressBookService;
import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.service.IpAddresses;

import java.util.Map;
import java.util.Optional;

public final class BanIpCommand extends AbstractAdminCommand {

    private final BanService bans;
    private final AddressBookService addresses;

    public BanIpCommand(
            PlatformService platform,
            UserService users,
            BanService bans,
            AddressBookService addresses
    ) {
        super(platform, users);
        this.bans = bans;
        this.addresses = addresses;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.banip";
    }

    @Override
    public String usage() {
        return "/banip <address|player> [reason]";
    }

    @Override
    public String name() {
        return "banip";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1) {
            invocation.errorKey(
                    "commands.admin.ban-ip.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var address = address(invocation.args()[0]);
        if (address.isEmpty()) {
            invocation.errorKey(
                    "commands.admin.ban-ip.unknown-address",
                    Map.of("target", invocation.args()[0])
            );
            return 0;
        }

        var result = bans.banIp(
                address.orElseThrow(),
                actor(invocation),
                join(invocation.args(), 1)
        );
        if (result.success()) invocation.reply(result.message());
        else invocation.error(result.message());
        return result.success() ? 1 : 0;
    }

    private Optional<String> address(String input) {
        var literal = IpAddresses.normalize(input);
        if (literal.isPresent()) return literal;

        var online = platform.onlinePlayer(input);
        if (online.isPresent()) return platform.address(online.orElseThrow());

        var resolved = users.findUuidByName(input);
        return resolved
                .flatMap(addresses::address)
                .or(() -> addresses.address(input));
    }

}
