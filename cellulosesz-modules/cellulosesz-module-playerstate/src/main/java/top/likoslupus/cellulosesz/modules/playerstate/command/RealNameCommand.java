package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class RealNameCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;

    public RealNameCommand(
            PlayerInformationCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    public static String normalize(String value) {
        return PlayerInformationCommandService.normalize(value);
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "realname",
                "cellulosesz.command.realname",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("realname")
                .then(Commands.argument(
                                        "nickname",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command ->
                                        PlayerStateCommandSupport.async(
                                                context,
                                                command,
                                                descriptor,
                                                "realname",
                                                policy -> service.realName(
                                                        PlayerStateCommandSupport.currentPlayer(
                                                                policy,
                                                                players
                                                        ),
                                                        StringArgumentType.getString(
                                                                command,
                                                                "nickname"
                                                        )
                                                )
                                        )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.realname",
                "/realname <nickname>",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
