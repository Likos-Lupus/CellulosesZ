package top.likoslupus.cellulosesz.modules.economy.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.economy.application.BalanceCommandService;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandSettings;
import top.likoslupus.cellulosesz.modules.economy.command.argument.MoneyAmounts;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class BalanceTopCommand implements CommandContributor {

    private final BalanceCommandService service;
    private final Supplier<EconomyCommandSettings> settings;

    public BalanceTopCommand(
            BalanceCommandService service,
            Supplier<EconomyCommandSettings> settings
    ) {
        this.service = requireNonNull(service, "service");
        this.settings = requireNonNull(settings, "settings");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "balancetop",
                "cellulosesz.economy.balancetop",
                CommandSourceKind.ANY
        );

        var snapshot = settings.get();

        var page = Commands.argument(
                        "page",
                        IntegerArgumentType.integer(1)
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        IntegerArgumentType.getInteger(
                                command,
                                "page"
                        ),
                        Optional.empty(),
                        Optional.empty()
                ))
                .then(Commands.argument(
                                        "minimum",
                                        StringArgumentType.word()
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        IntegerArgumentType.getInteger(
                                                command,
                                                "page"
                                        ),
                                        Optional.of(
                                                amount(command, "minimum", snapshot)
                                        ),
                                        Optional.empty()
                                ))
                                .then(Commands.argument(
                                                        "maximum",
                                                        StringArgumentType.word()
                                                )
                                                .executes(command -> execute(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        IntegerArgumentType.getInteger(
                                                                command,
                                                                "page"
                                                        ),
                                                        Optional.of(
                                                                amount(command, "minimum", snapshot)
                                                        ),
                                                        Optional.of(
                                                                amount(command, "maximum", snapshot)
                                                        )
                                                ))
                                )
                );

        var root = Commands.literal("balancetop")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        1,
                        Optional.empty(),
                        Optional.empty()
                ))
                .then(page);

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("baltop"),
                "commands.description.balancetop",
                "/balancetop [page] [minimum] [maximum]",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "baltop",
                node
        );
    }

    private static BigDecimal amount(
            CommandContext<CommandSourceStack> command,
            String name,
            EconomyCommandSettings settings
    ) throws CommandSyntaxException {
        return MoneyAmounts.nonNegative(
                StringArgumentType.getString(command, name),
                settings.scale(),
                settings.maximumBalance()
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int page,
            Optional<BigDecimal> minimum,
            Optional<BigDecimal> maximum
    ) {
        return EconomyCommandSupport.async(
                context,
                command,
                descriptor,
                "balance top",
                _ -> service.top(
                        page,
                        minimum,
                        maximum
                )
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}
