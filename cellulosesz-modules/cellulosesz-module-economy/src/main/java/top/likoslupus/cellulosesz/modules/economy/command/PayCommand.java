package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

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
        return "/pay <player> <amount> [confirmation]";
    }

    @Override
    public String name() {
        return "pay";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2 || args.length > 3) {
            invocation.errorKey(
                    "commands.economy.pay-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var sender = player(invocation);
        var amount = amount(invocation, args[1]);
        if (sender.isEmpty() || amount.isEmpty()) return 0;

        var resolved = players.resolveKnown(args[0], sender.get());
        if (resolved.state() == ResolvedPlayerState.UNKNOWN || resolved.optionalUuid().isEmpty()) {
            invocation.errorKey(
                    "commands.economy.abstract-economy-command.error.3",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        var targetUuid = resolved.optionalUuid().orElseThrow();
        if (targetUuid.equals(sender.get().uuid())) {
            invocation.errorKey("commands.economy.pay-self");
            return 0;
        }

        var minimum = decimal(config.pay.minimum, BigDecimal.valueOf(0.01));
        if (amount.get().compareTo(minimum) < 0) {
            invocation.errorKey(
                    "commands.economy.pay-command.error.2",
                    Map.of("value0", format(minimum))
            );
            return 0;
        }

        var recipientUser = users.load(targetUuid).join();
        if (!recipientUser.preferences.payments) {
            invocation.errorKey("commands.economy.pay-command.error.3");
            return 0;
        }

        var confirmationThreshold = decimal(config.pay.requireConfirmAbove, BigDecimal.ZERO);
        if (users.load(sender.get().uuid()).join().preferences.confirmLargePayments
                && confirmationThreshold.signum() > 0
                && amount.get().compareTo(confirmationThreshold) >= 0) {
            var pending = new PendingPayment(targetUuid, amount.get());
            if (args.length == 2) {
                var token = confirmations.request(
                        sender.get().uuid(),
                        CONFIRM_ACTION,
                        pending,
                        CONFIRM_TTL
                );
                invocation.replyKey(
                        "commands.economy.pay-confirm-required",
                        Map.of(
                                "player", resolved.name(),
                                "amount", format(amount.get()),
                                "token", token,
                                "seconds", CONFIRM_TTL.toSeconds()
                        )
                );
                return 1;
            }

            var confirmed = confirmations.consume(
                    sender.get().uuid(),
                    CONFIRM_ACTION,
                    args[2],
                    PendingPayment.class
            );
            if (confirmed.isEmpty()
                    || !confirmed.get().target().equals(targetUuid)
                    || confirmed.get().amount().compareTo(amount.get()) != 0) {
                invocation.errorKey("commands.economy.pay-confirm-invalid");
                return 0;
            }
        } else if (args.length == 3) {
            invocation.errorKey("commands.economy.pay-confirm-unexpected");
            return 0;
        }

        var result = economy.transfer(
                sender.get().uuid(),
                targetUuid,
                amount.get(),
                cause(invocation, "pay " + resolved.name())
        );
        if (!result.success()) {
            invocation.error(result.message());
            return 0;
        }

        invocation.replyKey(
                "commands.economy.pay-command.reply.1",
                Map.of(
                        "value0", resolved.name(),
                        "value1", format(amount.get()),
                        "value2", format(result.balance())
                )
        );
        resolved.online().ifPresent(targetPlayer -> platform.sendMessage(
                targetPlayer,
                messages.render(
                        locales.locale(targetPlayer),
                        "commands.economy.pay-received",
                        Map.of(
                                "player", sender.get().name(),
                                "amount", format(amount.get())
                        )
                )
        ));
        return 1;
    }

    private BigDecimal decimal(String value, BigDecimal fallback) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    private record PendingPayment(
            UUID target,
            BigDecimal amount
    ) {

    }

}
