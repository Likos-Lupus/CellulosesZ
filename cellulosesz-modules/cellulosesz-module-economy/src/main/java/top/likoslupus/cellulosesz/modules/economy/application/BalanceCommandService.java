package top.likoslupus.cellulosesz.modules.economy.application;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.economy.BalanceFilter;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.user.NameCacheService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public final class BalanceCommandService {

    private final EconomyService economy;
    private final PlayerResolver players;
    private final NameCacheService names;
    private final Supplier<EconomyCommandSettings> settings;

    public BalanceCommandService(
            EconomyService economy,
            PlayerResolver players,
            NameCacheService names,
            Supplier<EconomyCommandSettings> settings
    ) {
        this.economy = requireNonNull(economy, "economy");
        this.players = requireNonNull(players, "players");
        this.names = requireNonNull(names, "names");
        this.settings = requireNonNull(settings, "settings");
    }

    public CompletableFuture<EconomyCommandResult> self(CellPlayer player) {
        return CompletableFuture.completedFuture(EconomyCommandResult.success(
                "commands.economy.balance-command.reply.balance",
                Map.of("balance", economy.format(economy.balance(player.uuid())))
        ));
    }

    public CompletableFuture<EconomyCommandResult> other(String input, @Nullable CellPlayer viewer) {
        return players.resolve(input, viewer).thenApply(target ->
                target.state() == ResolvedPlayerState.UNKNOWN
                        || target.optionalUuid().isEmpty() ? EconomyCommandResult.failure(
                        "commands.economy.abstract-economy-command.error.player-not-found",
                        Map.of("player", input)
                ) : EconomyCommandResult.success(
                        "commands.economy.balance-other",
                        Map.of(
                                "player", target.name(),
                                "balance", economy.format(economy.balance(target.optionalUuid().orElseThrow()))
                        )
                )
        );
    }

    public CompletableFuture<EconomyCommandResult> top(
            int page,
            Optional<BigDecimal> minimum,
            Optional<BigDecimal> maximum
    ) {
        var snapshot = settings.get();
        if (minimum.isPresent() && maximum.isPresent()
                && minimum.orElseThrow().compareTo(maximum.orElseThrow()) > 0
        ) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure("commands.economy.balance-top.invalid-filter"));
        }

        final int limit;
        final int from;
        try {
            limit = Math.multiplyExact(page, snapshot.balanceTopPageSize());
            from = Math.multiplyExact(page - 1, snapshot.balanceTopPageSize());
        } catch (ArithmeticException failure) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure("commands.economy.balance-top-command.error.page-number-must-integer"));
        }

        var entries = economy.topBalances(limit, new BalanceFilter(minimum, maximum));
        if (entries.isEmpty() && page == 1) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure("commands.economy.balance-top.empty"));
        }
        if (from >= entries.size()) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure("commands.economy.balance-top-command.error.there-no-balance-entries-page"));
        }

        var nameSnapshot = names.entries();
        var messages = new ArrayList<LocalizedMessage>();
        messages.add(LocalizedMessage.of(
                "commands.economy.balance-top-header",
                Map.of("page", page)
        ));
        var end = Math.min(
                entries.size(),
                Math.addExact(from, snapshot.balanceTopPageSize())
        );

        IntStream.range(from, end)
                .forEach(index -> {
                    var entry = entries.get(index);
                    messages.add(LocalizedMessage.of("commands.economy.balance-top-row", Map.of(
                            "rank", index + 1,
                            "player", Optional.ofNullable(nameSnapshot.get(entry.uuid()))
                                    .filter(value -> !value.isBlank())
                                    .orElse(entry.uuid().toString()),
                            "balance", economy.format(entry.balance())
                    )));
                });

        return CompletableFuture.completedFuture(EconomyCommandResult.success(messages));
    }

    public CompletableFuture<EconomyCommandResult> mutate(
            Mutation mutation,
            String input,
            BigDecimal amount,
            String actor
    ) {
        return players.resolve(input, null).thenCompose(target -> {
            if (target.state() == ResolvedPlayerState.UNKNOWN || target.optionalUuid().isEmpty()) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "commands.economy.abstract-economy-command.error.player-not-found",
                        Map.of("player", input)
                ));
            }

            var cause = TransactionCause.command(
                    actor,
                    "eco " + mutation.name().toLowerCase()
            );
            var future = switch (mutation) {
                case GIVE -> economy.deposit(target.optionalUuid().orElseThrow(), amount, cause);
                case TAKE -> economy.withdraw(target.optionalUuid().orElseThrow(), amount, cause);
                case SET -> economy.setBalance(target.optionalUuid().orElseThrow(), amount, cause);
            };

            return future.thenApply(transaction ->
                    transaction.success() ? EconomyCommandResult.success(
                            "commands.economy.eco-result",
                            Map.of(
                                    "player", target.name(),
                                    "result", transaction.message(),
                                    "balance", economy.format(transaction.balance())
                            )
                    ) : EconomyCommandResult.failure(transaction.message()));
        });
    }

    public enum Mutation {

        GIVE,
        TAKE,
        SET

    }

}
