package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.admin.application.PlayerControlCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BurnCommand implements CommandContributor {

    private final PlayerControlCommandService service;
    private final PlayerDirectory players;
    private final int maximum;

    public BurnCommand(
            PlayerControlCommandService service,
            PlayerDirectory players,
            int maximum
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.maximum = maximum;
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "burn",
                "cellulosesz.command.burn",
                CommandSourceKind.ANY
        );

        var player = Commands.argument(
                        "player",
                        PlayerNameArgument.playerName()
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                players::onlinePlayerNames,
                                builder
                        )
                )
                .then(Commands.argument(
                                        "seconds",
                                        IntegerArgumentType.integer(0, maximum)
                                )
                                .executes(command -> AdminCommandResults.async(
                                        context,
                                        command,
                                        descriptor,
                                        "burn seconds="
                                                + IntegerArgumentType.getInteger(
                                                command,
                                                "seconds"
                                        ),
                                        _ -> service.burn(
                                                PlayerNameArgument.get(
                                                        command,
                                                        "player"
                                                ),
                                                IntegerArgumentType.getInteger(
                                                        command,
                                                        "seconds"
                                                )
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.burn",
                "/burn <player> <seconds>",
                Commands.literal("burn").then(player)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
