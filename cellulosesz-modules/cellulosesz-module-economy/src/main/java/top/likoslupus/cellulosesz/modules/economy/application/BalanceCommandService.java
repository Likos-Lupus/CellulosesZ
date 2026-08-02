package top.likoslupus.cellulosesz.modules.economy.application;

import top.likoslupus.cellulosesz.api.economy.BalanceFilter;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.NameCacheService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

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
                MessageArguments.builder()
                        .put("balance", economy.format(economy.balance(player.uuid())))
                        .build()
        ));
    }

    public CompletableFuture<EconomyCommandResult> other(
            String input,
            @Nullable CellPlayer viewer
    ) {
        return players.resolve(input, viewer).thenApply(target ->
                target.state() == ResolvedPlayerState.UNKNOWN || target.optionalUuid().isEmpty()
                        ?
                        EconomyCommandResult.failure(
                                "commands.economy.abstract-economy-command.error.player-not-found",
                                MessageArguments.builder().put("player", input).build()
                        )
                        : EconomyCommandResult.success(
                                "commands.economy.balance-other",
                                MessageArguments.builder()
                                        .put("player", target.name())
                                        .put(
                                                "balance",
                                                economy.format(economy.balance(target.optionalUuid()
                                                        .orElseThrow()))
                                        )
                                        .build()
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
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.balance-top.invalid-filter"
            ));
        }

        final int limit;
        final int from;

        try {
            limit = Math.multiplyExact(page, snapshot.balanceTopPageSize());
            from = Math.multiplyExact(page - 1, snapshot.balanceTopPageSize());
        } catch (ArithmeticException failure) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.balance-top-command.error.page-number-must-integer"
            ));
        }

        var entries = economy.topBalances(limit, new BalanceFilter(minimum, maximum));
        if (entries.isEmpty() && page == 1) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.balance-top.empty"
            ));
        }

        if (from >= entries.size()) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.balance-top-command.error.there-no-balance-entries-page"
            ));
        }

        var nameSnapshot = names.entries();
        var messages = new ArrayList<LocalizedMessage>();
        messages.add(LocalizedMessage.of(
                "commands.economy.balance-top-header",
                MessageArguments.builder().put("page", page).build()
        ));

        var end = Math.min(
                entries.size(),
                Math.addExact(from, snapshot.balanceTopPageSize())
        );
        IntStream.range(from, end)
                .forEach(index -> {
                    var entry = entries.get(index);
                    messages.add(LocalizedMessage.of(
                            "commands.economy.balance-top-row",
                            MessageArguments.builder().put("rank", index + 1).put(
                                    "player", Optional.ofNullable(nameSnapshot.get(entry.uuid()))
                                            .filter(value -> !value.isBlank())
                                            .orElse(entry.uuid().toString())
                            ).put("balance", economy.format(entry.balance())).build()
                    ));
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
                        MessageArguments.builder().put("player", input).build()
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
                    transaction.success()
                            ?
                            EconomyCommandResult.success(
                                    "commands.economy.eco-result",
                                    MessageArguments.builder()
                                            .put("player", target.name())
                                            .put("result", transaction.message())
                                            .put("balance", economy.format(transaction.balance()))
                                            .build()
                            )
                            : EconomyCommandResult.failure(transaction.message()));
        });
    }

    public enum Mutation {

        GIVE,
        TAKE,
        SET

    }

}
