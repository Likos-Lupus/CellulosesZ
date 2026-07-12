package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;
import top.likoslupus.cellulosesz.modules.economy.service.JsonEconomyService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

abstract class AbstractEconomyCommand implements CellCommand {

    protected final PlatformService platform;
    protected final UserService users;
    protected final EconomyService economy;
    protected final EconomyConfig config;

    AbstractEconomyCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        this.platform = platform;
        this.users = users;
        this.economy = economy;
        this.config = config;
    }

    protected Optional<CellPlayer> player(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) invocation.errorKey("commands.economy.abstract-economy-command.error.1");
        return player;
    }

    protected Optional<CellPlayer> online(CommandInvocation invocation, String name) {
        var player = invocation.resolvePlayer(name).online();
        if (player.isEmpty()) {
            invocation.errorKey(
                    "commands.economy.abstract-economy-command.error.2",
                    Map.of("value0", name)
            );
        }
        return player;
    }

    protected Optional<UUID> uuid(CommandInvocation invocation, String name) {
        var uuid = invocation.resolvePlayer(name).optionalUuid();
        if (uuid.isEmpty()) {
            invocation.errorKey(
                    "commands.economy.abstract-economy-command.error.3",
                    Map.of("value0", name)
            );
        }
        return uuid;
    }

    protected Optional<BigDecimal> amount(CommandInvocation invocation, String value) {
        try {
            var amount = new BigDecimal(value);
            if (amount.signum() <= 0) {
                invocation.errorKey("commands.economy.abstract-economy-command.error.4");
                return Optional.empty();
            }
            return Optional.of(amount);
        } catch (NumberFormatException _) {
            invocation.errorKey(
                    "commands.economy.abstract-economy-command.error.5",
                    Map.of("value0", value)
            );
            return Optional.empty();
        }
    }

    protected TransactionCause cause(CommandInvocation invocation, String note) {
        return TransactionCause.command(invocation.playerName().orElse("console"), note);
    }

    protected String format(BigDecimal amount) {
        if (economy instanceof JsonEconomyService service) {
            return service.format(amount);
        }
        return config.currency.symbol + amount.setScale(config.currency.scale, RoundingMode.HALF_UP).toPlainString();
    }

}
