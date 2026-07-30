package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class NickCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public NickCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "nick",
                "cellulosesz.playerstate.nick",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("nick")
                .then(Commands.literal("off")
                        .executes(command ->
                                PlayerStateCommandSupport.requirePlayer(
                                        context,
                                        command,
                                        descriptor,
                                        "nick clear",
                                        players,
                                        player -> service.nick(
                                                player,
                                                Optional.empty()
                                        )
                                )
                        )
                )
                .then(Commands.argument(
                                        "name",
                                        StringArgumentType.word()
                                )
                                .executes(command ->
                                        PlayerStateCommandSupport.requirePlayer(
                                                context,
                                                command,
                                                descriptor,
                                                "nick set",
                                                players,
                                                player -> service.nick(
                                                        player,
                                                        Optional.of(
                                                                StringArgumentType.getString(
                                                                        command,
                                                                        "name"
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
                "commands.description.nick",
                "/nick <name|off>",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
