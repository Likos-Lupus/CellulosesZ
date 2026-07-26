package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.AddressBookService;
import top.likoslupus.cellulosesz.api.admin.TempBanService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.service.DurationParser;
import top.likoslupus.cellulosesz.modules.admin.service.IpAddresses;

import java.util.Map;
import java.util.Optional;

public final class TempBanIpCommand extends AbstractAdminCommand {

    private final TempBanService bans;
    private final AddressBookService addresses;
    private final AdminConfig config;

    public TempBanIpCommand(
            PlatformService platform,
            UserService users,
            TempBanService bans,
            AddressBookService addresses,
            AdminConfig config
    ) {
        super(platform, users);
        this.bans = bans;
        this.addresses = addresses;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.tempbanip";
    }

    @Override
    public String usage() {
        return "/tempbanip <address|player> <duration> [reason]";
    }

    @Override
    public String name() {
        return "tempbanip";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();

        if (args.length < 2) {
            invocation.errorKey(
                    "commands.admin.temp-ban-ip.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var duration = DurationParser.parseMillis(args[1]);
        if (duration.isEmpty()) {
            invocation.errorKey("commands.admin.temp-ban-command.error.2");
            return 0;
        }

        if (exceedsMaximum(duration.getAsLong())
                && !invocation.hasPermission("cellulosesz.admin.punishment.unlimited")
        ) {
            invocation.errorKey(
                    "commands.admin.maximum-punishment",
                    Map.of("seconds", config.maximumPunishmentSeconds)
            );
            return 0;
        }

        var address = address(args[0]);
        if (address.isEmpty()) {
            invocation.errorKey(
                    "commands.admin.ban-ip.unknown-address",
                    Map.of("target", args[0])
            );
            return 0;
        }

        bans.tempBanIp(
                address.orElseThrow(),
                actor(invocation),
                duration.getAsLong(),
                join(args, 2)
        ).whenComplete((result, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.admin.persistence-failed");
            } else if (result.success()) {
                invocation.reply(result.message());
            } else {
                invocation.error(result.message());
            }
        });
        return 1;
    }

    private boolean exceedsMaximum(long durationMillis) {
        if (config.maximumPunishmentSeconds < 0) return false;
        try {
            return durationMillis > Math.multiplyExact(config.maximumPunishmentSeconds, 1000L);
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private Optional<String> address(String input) {
        var literal = IpAddresses.normalize(input);
        if (literal.isPresent()) return literal;

        var online = platform.onlinePlayer(input);
        if (online.isPresent()) return platform.address(online.orElseThrow());

        return users.findUuidByName(input)
                .flatMap(addresses::address)
                .or(() -> addresses.address(input));
    }

}
