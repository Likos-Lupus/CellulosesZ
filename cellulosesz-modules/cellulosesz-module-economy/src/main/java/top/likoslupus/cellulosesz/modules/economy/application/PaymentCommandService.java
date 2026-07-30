package top.likoslupus.cellulosesz.modules.economy.application;

import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class PaymentCommandService {

    private static final String CONFIRM_ACTION = "economy.pay";
    private static final Duration CONFIRM_TTL = Duration.ofSeconds(30);

    private final EconomyService economy;
    private final UserService users;
    private final PlayerResolver players;
    private final ConfirmationService confirmations;
    private final PlayerAudienceService audiences;
    private final MessageRenderer messages;
    private final Supplier<EconomyCommandSettings> settings;

    public PaymentCommandService(
            EconomyService economy,
            UserService users,
            PlayerResolver players,
            ConfirmationService confirmations,
            PlayerAudienceService audiences,
            MessageRenderer messages,
            Supplier<EconomyCommandSettings> settings
    ) {
        this.economy = requireNonNull(economy, "economy");
        this.users = requireNonNull(users, "users");
        this.players = requireNonNull(players, "players");
        this.confirmations = requireNonNull(confirmations, "confirmations");
        this.audiences = requireNonNull(audiences, "audiences");
        this.messages = requireNonNull(messages, "messages");
        this.settings = requireNonNull(settings, "settings");
    }

    public CompletableFuture<EconomyCommandResult> pay(
            CellPlayer sender,
            List<String> targetTokens,
            BigDecimal amount,
            Optional<String> token,
            boolean multiplePermission,
            boolean offlinePermission
    ) {
        var snapshot = settings.get();
        if (amount.compareTo(snapshot.minimumPayment()) < 0) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.pay-command.error.payment-amount-cannot-less-than",
                    Map.of("minimum_amount", economy.format(snapshot.minimumPayment()))
            ));
        }

        if (targetTokens.size() > 1 && !multiplePermission) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.pay-multiple-denied"
            ));
        }

        if (targetTokens.size() > snapshot.maximumRecipients()) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.pay-too-many",
                    Map.of("maximum", snapshot.maximumRecipients())
            ));
        }
        return resolveTargets(sender, targetTokens)
                .thenCompose(resolved -> validateAndPay(
                        sender,
                        resolved,
                        amount,
                        token,
                        offlinePermission,
                        snapshot
                ));
    }

    private CompletableFuture<List<ResolvedPlayer>> resolveTargets(
            CellPlayer sender,
            List<String> tokens
    ) {
        CompletableFuture<List<ResolvedPlayer>> future = CompletableFuture.completedFuture(new ArrayList<>());
        for (var token : tokens) {
            future = future.thenCombine(
                    players.resolve(token, sender),
                    (current, resolved) -> {
                        var next = new ArrayList<>(current);
                        next.add(resolved);
                        return next;
                    }
            );
        }

        return future.thenApply(resolved -> {
            var unique = new LinkedHashMap<UUID, ResolvedPlayer>();
            resolved.forEach(target ->
                    target.optionalUuid()
                            .ifPresent(uuid -> unique.putIfAbsent(uuid, target))
            );
            return List.copyOf(unique.values());
        });
    }

    private CompletableFuture<EconomyCommandResult> validateAndPay(
            CellPlayer sender, List<ResolvedPlayer> resolved,
            BigDecimal amount, Optional<String> token,
            boolean offlinePermission,
            EconomyCommandSettings snapshot
    ) {
        if (resolved.isEmpty()) return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                "commands.economy.abstract-economy-command.error.player-not-found"
        ));

        for (var target : resolved) {
            if (target.state() == ResolvedPlayerState.UNKNOWN || target.optionalUuid().isEmpty()) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "commands.economy.abstract-economy-command.error.player-not-found",
                        Map.of("player", target.name())
                ));
            }

            if (target.optionalUuid().orElseThrow().equals(sender.uuid())) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "commands.economy.pay-self"
                ));
            }

            if (target.state() == ResolvedPlayerState.OFFLINE
                    && !snapshot.allowOfflineByDefault()
                    && !offlinePermission
            ) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "commands.economy.pay-offline-denied",
                        Map.of("player", target.name())
                ));
            }
        }

        var ids = new ArrayList<UUID>();
        ids.add(sender.uuid());
        resolved.forEach(target -> ids.add(target.optionalUuid().orElseThrow()));
        return loadUsers(ids)
                .thenCompose(loaded -> continuePayment(
                        sender,
                        resolved,
                        amount,
                        token,
                        snapshot,
                        loaded
                ));
    }

    private CompletableFuture<Map<UUID, CellUser>> loadUsers(List<UUID> ids) {
        CompletableFuture<Map<UUID, CellUser>> future = CompletableFuture.completedFuture(new LinkedHashMap<>());
        for (var id : ids.stream().distinct().toList()) {
            future = future.thenCombine(users.load(id), (loaded, user) -> {
                var copy = new LinkedHashMap<>(loaded);
                copy.put(id, user);
                return copy;
            });
        }
        return future.thenApply(Map::copyOf);
    }

    private CompletableFuture<EconomyCommandResult> continuePayment(
            CellPlayer sender,
            List<ResolvedPlayer> resolved,
            BigDecimal amount,
            Optional<String> token,
            EconomyCommandSettings snapshot,
            Map<UUID, CellUser> loaded
    ) {
        for (var target : resolved) {
            var recipient = loaded.get(target.optionalUuid().orElseThrow());
            if (recipient == null) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "service.user.load-failed"
                ));
            }

            if (!recipient.preferences.payments) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "commands.economy.pay-command.error.player-not-accepting-payments",
                        Map.of("player", target.name())
                ));
            }

            if (snapshot.respectIgnore()
                    && recipient.relations.ignored.contains(sender.uuid())
            ) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "commands.economy.pay-ignored",
                        Map.of("player", target.name())
                ));
            }
        }

        var senderUser = loaded.get(sender.uuid());
        if (senderUser == null) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "service.user.load-failed"
            ));
        }

        var recipientIds = resolved.stream()
                .map(target -> target.optionalUuid().orElseThrow())
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        var total = amount.multiply(BigDecimal.valueOf(recipientIds.size()));
        var payload = new PendingPayment(
                sender.uuid(),
                recipientIds,
                amount,
                total,
                snapshot.version()
        );

        if (senderUser.preferences.confirmLargePayments
                && snapshot.confirmationThreshold().signum() > 0
                && total.compareTo(snapshot.confirmationThreshold()) >= 0
        ) {
            if (token.isEmpty()) {
                var generated = confirmations.request(
                        sender.uuid(),
                        CONFIRM_ACTION,
                        payload,
                        CONFIRM_TTL
                );
                return CompletableFuture.completedFuture(EconomyCommandResult.success(
                        "commands.economy.pay-confirm-required",
                        Map.of(
                                "player", resolved.stream()
                                        .map(ResolvedPlayer::name)
                                        .sorted(String.CASE_INSENSITIVE_ORDER)
                                        .toList(),
                                "amount", economy.format(total),
                                "token", generated,
                                "seconds", CONFIRM_TTL.toSeconds()
                        )
                ));
            }

            var confirmed = confirmations.consume(
                    sender.uuid(),
                    CONFIRM_ACTION,
                    token.orElseThrow(),
                    PendingPayment.class
            );
            if (confirmed.isEmpty()
                    || !confirmed.orElseThrow().equals(payload)
            ) {
                return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                        "commands.economy.pay-confirm-invalid"
                ));
            }
        } else if (token.isPresent()) {
            return CompletableFuture.completedFuture(EconomyCommandResult.failure(
                    "commands.economy.pay-confirm-unexpected"
            ));
        }

        var names = resolved.stream()
                .map(ResolvedPlayer::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return economy.transferMany(
                        sender.uuid(),
                        recipientIds,
                        amount,
                        TransactionCause.command(
                                sender.name(),
                                "pay recipients=" + recipientIds.size()
                        )
                )
                .thenApply(transaction -> {
                    if (!transaction.success()) {
                        return EconomyCommandResult.failure(transaction.message());
                    }
                    resolved.forEach(target ->
                            target.online().ifPresent(player -> audiences.send(
                                    player,
                                    messages.render(
                                            audiences.locale(player),
                                            "commands.economy.pay-received",
                                            Map.of(
                                                    "player", sender.name(),
                                                    "amount", economy.format(amount))
                                    )
                            ))
                    );
                    return EconomyCommandResult.success(
                            "commands.economy.pay-command.reply.paid-current-balance",
                            Map.of(
                                    "recipients", names,
                                    "amount", economy.format(total),
                                    "balance", economy.format(transaction.balance())
                            )
                    );
                });
    }

    public CompletableFuture<EconomyCommandResult> togglePayments(UUID uuid) {
        return users.update(uuid, user -> user.preferences.payments = !user.preferences.payments)
                .thenApply(enabled -> EconomyCommandResult.success(
                        enabled
                                ? "commands.economy.payments-enabled"
                                : "commands.economy.payments-disabled"
                ));
    }

    public CompletableFuture<EconomyCommandResult> toggleConfirmation(UUID uuid) {
        return users.update(uuid, user -> user.preferences.confirmLargePayments = !user.preferences.confirmLargePayments)
                .thenApply(enabled -> {
                    confirmations.clear(uuid, CONFIRM_ACTION);
                    return EconomyCommandResult.success(
                            enabled
                                    ? "commands.economy.pay-confirm-enabled"
                                    : "commands.economy.pay-confirm-disabled"
                    );
                });
    }

    public record PendingPayment(
            UUID sender,
            List<UUID> recipients,
            BigDecimal amountEach,
            BigDecimal total,
            long configVersion
    ) {

        public PendingPayment {
            requireNonNull(sender, "sender");
            recipients = List.copyOf(requireNonNull(recipients, "recipients"));
            requireNonNull(amountEach, "amountEach");
            requireNonNull(total, "total");
        }

    }

}
