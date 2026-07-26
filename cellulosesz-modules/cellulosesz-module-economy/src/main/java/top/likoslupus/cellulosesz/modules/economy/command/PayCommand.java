package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class PayCommand extends AbstractEconomyCommand {

    private static final String CONFIRM_ACTION = "economy.pay";
    private static final Duration CONFIRM_TTL = Duration.ofSeconds(30);

    private final PlayerResolver players;
    private final ConfirmationService confirmations;
    private final MessageRenderer messages;
    private final LocaleResolver locales;

    public PayCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config,
            PlayerResolver players,
            ConfirmationService confirmations,
            MessageRenderer messages,
            LocaleResolver locales
    ) {
        super(platform, users, economy, config);
        this.players = players;
        this.confirmations = confirmations;
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.pay";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/pay <player[,player...]> <amount> [confirmation]";
    }

    @Override
    public String name() {
        return "pay";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2 || args.length > 3) {
            invocation.errorKey("commands.economy.pay-command.error.1", Map.of("value0", usage()));
            return 0;
        }

        var sender = player(invocation);
        var amount = amount(invocation, args[1]);
        if (sender.isEmpty() || amount.isEmpty()) return 0;

        var minimum = decimal(config.pay.minimum, BigDecimal.valueOf(0.01));
        if (amount.orElseThrow().compareTo(minimum) < 0) {
            invocation.errorKey(
                    "commands.economy.pay-command.error.2",
                    Map.of("value0", format(minimum))
            );
            return 0;
        }

        var resolvedTargets = resolveTargets(invocation, sender.orElseThrow(), args[0]);
        if (resolvedTargets.isEmpty()) return 0;
        if (resolvedTargets.size() > 1 && !invocation.hasPermission("cellulosesz.economy.pay.multiple")) {
            invocation.errorKey("commands.economy.pay-multiple-denied");
            return 0;
        }
        if (resolvedTargets.size() > Math.max(1, config.pay.maximumRecipients)) {
            invocation.errorKey(
                    "commands.economy.pay-too-many",
                    Map.of("maximum", config.pay.maximumRecipients)
            );
            return 0;
        }

        var recipientIds = resolvedTargets.stream()
                .map(target -> target.optionalUuid().orElseThrow())
                .toList();
        for (var target : resolvedTargets) {
            var uuid = target.optionalUuid().orElseThrow();
            if (uuid.equals(sender.orElseThrow().uuid())) {
                invocation.errorKey("commands.economy.pay-self");
                return 0;
            }
            if (target.state() == ResolvedPlayerState.OFFLINE
                    && !config.pay.allowOfflineByDefault
                    && !invocation.hasPermission("cellulosesz.economy.pay.offline")) {
                invocation.errorKey(
                        "commands.economy.pay-offline-denied",
                        Map.of("player", target.name())
                );
                return 0;
            }
        }

        var ids = new ArrayList<UUID>();
        ids.add(sender.orElseThrow().uuid());
        ids.addAll(recipientIds);
        loadUsers(ids).whenComplete((loaded, failure) -> platform.runOnServerThread(() -> {
            if (failure != null) {
                invocation.errorKey("service.user.load-failed");
                return;
            }
            continuePayment(
                    invocation,
                    sender.orElseThrow(),
                    resolvedTargets,
                    amount.orElseThrow(),
                    args.length == 3 ? Optional.of(args[2]) : Optional.empty(),
                    loaded
            );
        }));
        return 1;
    }

    private BigDecimal decimal(String value, BigDecimal fallback) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    private List<ResolvedPlayer> resolveTargets(
            CommandInvocation invocation,
            CellPlayer sender,
            String input
    ) {
        var unique = new LinkedHashMap<UUID, ResolvedPlayer>();
        for (var token : input.split(",")) {
            var name = token.trim();
            if (name.isEmpty()) continue;
            var resolved = players.resolveKnown(name, sender);
            if (resolved.state() == ResolvedPlayerState.UNKNOWN || resolved.optionalUuid().isEmpty()) {
                invocation.errorKey(
                        "commands.economy.abstract-economy-command.error.3",
                        Map.of("value0", name)
                );
                return List.of();
            }
            unique.putIfAbsent(resolved.optionalUuid().orElseThrow(), resolved);
        }
        if (unique.isEmpty()) {
            invocation.errorKey("commands.economy.pay-command.error.1", Map.of("value0", usage()));
            return List.of();
        }
        return List.copyOf(unique.values());
    }

    private CompletableFuture<Map<UUID, CellUser>> loadUsers(List<UUID> ids) {
        CompletableFuture<Map<UUID, CellUser>> result =
                CompletableFuture.completedFuture(new LinkedHashMap<>());
        for (var id : ids.stream().distinct().toList()) {
            result = result.thenCombine(users.load(id), (loaded, user) -> {
                var copy = new LinkedHashMap<>(loaded);
                copy.put(id, user);
                return copy;
            });
        }
        return result.thenApply(Map::copyOf);
    }

    private void continuePayment(
            CommandInvocation invocation,
            CellPlayer sender,
            List<ResolvedPlayer> resolvedTargets,
            BigDecimal amount,
            Optional<String> confirmationToken,
            Map<UUID, CellUser> loaded
    ) {
        for (var target : resolvedTargets) {
            var recipientUser = loaded.get(target.optionalUuid().orElseThrow());
            if (recipientUser == null) {
                invocation.errorKey("service.user.load-failed");
                return;
            }
            if (!recipientUser.preferences.payments) {
                invocation.errorKey(
                        "commands.economy.pay-command.error.3",
                        Map.of("player", target.name())
                );
                return;
            }
            if (config.pay.respectIgnore && recipientUser.relations.ignored.contains(sender.uuid())) {
                invocation.errorKey(
                        "commands.economy.pay-ignored",
                        Map.of("player", target.name())
                );
                return;
            }
        }

        var senderUser = loaded.get(sender.uuid());
        if (senderUser == null) {
            invocation.errorKey("service.user.load-failed");
            return;
        }
        var recipientIds = resolvedTargets.stream()
                .map(target -> target.optionalUuid().orElseThrow())
                .toList();
        var names = resolvedTargets.stream().map(ResolvedPlayer::name).toList();
        var total = amount.multiply(BigDecimal.valueOf(recipientIds.size()));
        var confirmationThreshold = decimal(config.pay.requireConfirmAbove, BigDecimal.ZERO);
        if (senderUser.preferences.confirmLargePayments
                && confirmationThreshold.signum() > 0
                && total.compareTo(confirmationThreshold) >= 0) {
            var pending = new PendingPayment(recipientIds, amount);
            if (confirmationToken.isEmpty()) {
                var token = confirmations.request(
                        sender.uuid(), CONFIRM_ACTION, pending, CONFIRM_TTL
                );
                invocation.replyKey(
                        "commands.economy.pay-confirm-required",
                        Map.of(
                                "player", String.join(", ", names),
                                "amount", format(total),
                                "token", token,
                                "seconds", CONFIRM_TTL.toSeconds()
                        )
                );
                return;
            }
            var confirmed = confirmations.consume(
                    sender.uuid(), CONFIRM_ACTION, confirmationToken.orElseThrow(), PendingPayment.class
            );
            if (confirmed.isEmpty()
                    || !confirmed.orElseThrow().targets().equals(recipientIds)
                    || confirmed.orElseThrow().amountEach().compareTo(amount) != 0) {
                invocation.errorKey("commands.economy.pay-confirm-invalid");
                return;
            }
        } else if (confirmationToken.isPresent()) {
            invocation.errorKey("commands.economy.pay-confirm-unexpected");
            return;
        }

        economy.transferMany(
                sender.uuid(),
                recipientIds,
                amount,
                cause(invocation, "pay " + String.join(",", names))
        ).whenComplete((result, failure) -> platform.runOnServerThread(() -> {
            if (failure != null) {
                invocation.errorKey("service.economy.persistence-failed");
                return;
            }
            if (!result.success()) {
                invocation.error(result.message());
                return;
            }
            invocation.replyKey(
                    "commands.economy.pay-command.reply.1",
                    Map.of(
                            "value0", String.join(", ", names),
                            "value1", format(total),
                            "value2", format(result.balance())
                    )
            );
            resolvedTargets.forEach(target -> target.online().ifPresent(targetPlayer ->
                    platform.sendMessage(
                            targetPlayer,
                            messages.render(
                                    locales.locale(targetPlayer),
                                    "commands.economy.pay-received",
                                    Map.of("player", sender.name(), "amount", format(amount))
                            )
                    )
            ));
        }));
    }

    private record PendingPayment(
            List<UUID> targets,
            BigDecimal amountEach
    ) {

        private PendingPayment {
            targets = List.copyOf(targets);
        }

    }

}
