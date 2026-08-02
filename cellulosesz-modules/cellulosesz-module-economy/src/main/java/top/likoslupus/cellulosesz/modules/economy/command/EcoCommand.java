package top.likoslupus.cellulosesz.modules.economy.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.economy.application.BalanceCommandService;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandSettings;
import top.likoslupus.cellulosesz.modules.economy.command.argument.MoneyArgument;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class EcoCommand implements CommandContributor {

    private final BalanceCommandService service;
    private final Supplier<EconomyCommandSettings> settings;

    public EcoCommand(
            BalanceCommandService service,
            Supplier<EconomyCommandSettings> settings
    ) {
        this.service = requireNonNull(service, "service");
        this.settings = requireNonNull(settings, "settings");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "eco",
                "cellulosesz.economy.admin",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("eco")
                .then(branch(
                        context,
                        descriptor,
                        "give",
                        BalanceCommandService.Mutation.GIVE,
                        true
                ))
                .then(branch(
                        context,
                        descriptor,
                        "take",
                        BalanceCommandService.Mutation.TAKE,
                        true
                ))
                .then(branch(
                        context,
                        descriptor,
                        "set",
                        BalanceCommandService.Mutation.SET,
                        false
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.eco",
                "/eco <give|take|set> <player> <amount>",
                root
        );
    }

    private LiteralArgumentBuilder<CommandSourceStack> branch(
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            String literal,
            BalanceCommandService.Mutation mutation,
            boolean positive
    ) {
        var snapshot = settings.get();

        var money = positive
                ? MoneyArgument.positive(
                snapshot.scale(),
                snapshot.maximumBalance()
        )
                : MoneyArgument.nonNegative(
                        snapshot.scale(),
                        snapshot.maximumBalance()
                );

        return Commands.literal(literal)
                .then(Commands.argument(
                                        "player",
                                        StringArgumentType.word()
                                )
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                context::onlinePlayerNames,
                                                builder
                                        )
                                )
                                .then(Commands.argument("amount", money)
                                        .executes(command ->
                                                EconomyCommandSupport.async(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        "eco " + literal,
                                                        policy -> service.mutate(
                                                                mutation,
                                                                StringArgumentType.getString(
                                                                        command,
                                                                        "player"
                                                                ),
                                                                MoneyArgument.get(
                                                                        command,
                                                                        "amount"
                                                                ),
                                                                policy.playerName()
                                                                        .orElse(
                                                                                "console"
                                                                        )
                                                        )
                                                )
                                        )
                                )
                );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}
