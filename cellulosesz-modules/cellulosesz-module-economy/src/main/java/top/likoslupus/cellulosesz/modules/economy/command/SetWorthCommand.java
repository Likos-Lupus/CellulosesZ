package top.likoslupus.cellulosesz.modules.economy.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandSettings;
import top.likoslupus.cellulosesz.modules.economy.application.ItemValueCommandService;
import top.likoslupus.cellulosesz.modules.economy.command.argument.MoneyArgument;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class SetWorthCommand implements CommandContributor {

    private final ItemValueCommandService service;
    private final Supplier<EconomyCommandSettings> settings;

    public SetWorthCommand(
            ItemValueCommandService service,
            Supplier<EconomyCommandSettings> settings
    ) {
        this.service = requireNonNull(service, "service");
        this.settings = requireNonNull(settings, "settings");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "setworth",
                "cellulosesz.economy.setworth",
                CommandSourceKind.ANY
        );

        var snapshot = settings.get();

        var root = Commands.literal("setworth")
                .then(Commands.argument(
                                        "item",
                                        StringArgumentType.word()
                                )
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                service::itemNames,
                                                builder
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .executes(command ->
                                                EconomyCommandSupport.async(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        "remove worth",
                                                        _ -> service.removeWorth(
                                                                StringArgumentType
                                                                        .getString(
                                                                                command,
                                                                                "item"
                                                                        )
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.argument(
                                                        "amount",
                                                        MoneyArgument.nonNegative(
                                                                snapshot.scale(),
                                                                snapshot.maximumBalance()
                                                        )
                                                )
                                                .executes(command ->
                                                        EconomyCommandSupport.async(
                                                                context,
                                                                command,
                                                                descriptor,
                                                                "set worth",
                                                                _ -> service.setWorth(
                                                                        StringArgumentType
                                                                                .getString(
                                                                                        command,
                                                                                        "item"
                                                                                ),
                                                                        MoneyArgument.get(
                                                                                command,
                                                                                "amount"
                                                                        )
                                                                )
                                                        )
                                                )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.setworth",
                "/setworth <item> <amount|remove>",
                root
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}
