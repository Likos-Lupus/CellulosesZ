package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class BalanceTopCommand extends AbstractEconomyCommand {

    public BalanceTopCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        super(platform, users, economy, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("baltop");
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.balancetop";
    }

    @Override
    public String usage() {
        return "/balancetop [page] [minimum] [maximum]";
    }

    @Override
    public String name() {
        return "balancetop";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 3) {
            invocation.errorKey("common.usage", Map.of("usage", usage()));
            return 0;
        }

        final int page;
        final Optional<BigDecimal> minimum;
        final Optional<BigDecimal> maximum;
        try {
            page = invocation.args().length >= 1 ? Integer.parseInt(invocation.args()[0]) : 1;
            if (page < 1) throw new NumberFormatException();
            minimum = invocation.args().length >= 2
                    ? Optional.of(new BigDecimal(invocation.args()[1]))
                    : Optional.empty();
            maximum = invocation.args().length >= 3
                    ? Optional.of(new BigDecimal(invocation.args()[2]))
                    : Optional.empty();
        } catch (NumberFormatException _) {
            invocation.errorKey("commands.economy.balance-top-command.error.1");
            return 0;
        }

        if (minimum.isPresent() && maximum.isPresent()
                && minimum.orElseThrow().compareTo(maximum.orElseThrow()) > 0) {
            invocation.errorKey("commands.economy.balance-top.invalid-filter");
            return 0;
        }

        var pageSize = Math.max(1, config.balanceTop.pageSize);
        final int limit;
        final int from;
        try {
            limit = Math.multiplyExact(page, pageSize);
            from = Math.multiplyExact(page - 1, pageSize);
        } catch (ArithmeticException _) {
            invocation.errorKey("commands.economy.balance-top-command.error.1");
            return 0;
        }

        var entries = economy.topBalances(limit, minimum.orElse(null), maximum.orElse(null));
        if (from >= entries.size()) {
            invocation.errorKey("commands.economy.balance-top-command.error.2");
            return 0;
        }

        var pageEntries = entries.subList(from, Math.min(entries.size(), from + pageSize));
        var loads = new ArrayList<CompletableFuture<Optional<CellUser>>>(pageEntries.size());
        for (var entry : pageEntries) {
            loads.add(users.load(entry.uuid())
                    .thenApply(Optional::of)
                    .exceptionally(_ -> Optional.empty()));
        }

        CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).whenComplete((_, failure) -> {
            var rows = new StringBuilder();
            for (int offset = 0; offset < pageEntries.size(); offset++) {
                var entry = pageEntries.get(offset);
                var loaded = loads.get(offset).getNow(Optional.empty());
                var name = loaded.map(user -> user.lastKnownName)
                        .filter(value -> !value.isBlank())
                        .orElse(entry.uuid().toString());
                rows.append("\n")
                        .append(from + offset + 1)
                        .append(". ")
                        .append(name)
                        .append(" - ")
                        .append(format(entry.balance()));
            }
            invocation.replyKey(
                    "commands.economy.balance-top",
                    Map.of("page", page, "rows", rows.toString())
            );
        });
        return 1;
    }

}
