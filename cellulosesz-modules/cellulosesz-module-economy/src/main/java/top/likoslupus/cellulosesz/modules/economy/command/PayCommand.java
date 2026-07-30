package top.likoslupus.cellulosesz.modules.economy.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandSettings;
import top.likoslupus.cellulosesz.modules.economy.application.PaymentCommandService;
import top.likoslupus.cellulosesz.modules.economy.command.argument.MoneyArgument;
import top.likoslupus.cellulosesz.modules.economy.command.argument.PaymentTargetsArgument;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class PayCommand implements CommandContributor {

    private final PaymentCommandService service;
    private final PlayerDirectory players;
    private final Supplier<EconomyCommandSettings> settings;

    public PayCommand(
            PaymentCommandService service,
            PlayerDirectory players,
            Supplier<EconomyCommandSettings> settings
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.settings = requireNonNull(settings, "settings");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "pay",
                "cellulosesz.economy.pay",
                CommandSourceKind.PLAYER_ONLY
        );

        var snapshot = settings.get();

        var root = Commands.literal("pay")
                .then(Commands.argument(
                                        "targets",
                                        PaymentTargetsArgument.targets(
                                                snapshot.maximumRecipients()
                                        )
                                )
                                .then(Commands.argument(
                                                        "amount",
                                                        MoneyArgument.positive(
                                                                snapshot.scale(),
                                                                snapshot.maximumBalance()
                                                        )
                                                )
                                                .executes(command -> execute(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        Optional.empty()
                                                ))
                                                .then(Commands.argument(
                                                                        "confirmation",
                                                                        StringArgumentType.word()
                                                                )
                                                                .executes(command -> execute(
                                                                        context,
                                                                        command,
                                                                        descriptor,
                                                                        Optional.of(
                                                                                StringArgumentType.getString(
                                                                                        command,
                                                                                        "confirmation"
                                                                                )
                                                                        )
                                                                ))
                                                )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.pay",
                "/pay <targets> <amount> [confirmation]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<String> token
    ) {
        return EconomyCommandSupport.requirePlayer(
                context,
                command,
                descriptor,
                "pay payload redacted",
                players,
                sender -> service.pay(
                        sender,
                        PaymentTargetsArgument.get(
                                command,
                                "targets"
                        ),
                        MoneyArgument.get(
                                command,
                                "amount"
                        ),
                        token,
                        context.permissions().has(
                                sender,
                                "cellulosesz.economy.pay.multiple"
                        ),
                        context.permissions().has(
                                sender,
                                "cellulosesz.economy.pay.offline"
                        )
                )
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}
