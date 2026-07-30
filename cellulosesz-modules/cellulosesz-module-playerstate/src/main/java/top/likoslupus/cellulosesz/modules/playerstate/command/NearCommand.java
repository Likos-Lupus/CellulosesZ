package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandSettings;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class NearCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;

    private volatile PlayerStateCommandSettings settings;

    public NearCommand(
            PlayerInformationCommandService service,
            PlayerDirectory players,
            PlayerStateCommandSettings settings
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.settings = requireNonNull(settings, "settings");
    }

    public void configure(PlayerStateCommandSettings replacement) {
        settings = requireNonNull(replacement, "replacement");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "near",
                "cellulosesz.playerstate.near",
                CommandSourceKind.PLAYER_ONLY
        );

        var current = settings;

        var root = Commands.literal("near")
                .executes(command ->
                        PlayerStateCommandSupport.requirePlayer(
                                context,
                                command,
                                descriptor,
                                "near",
                                players,
                                player -> service.near(
                                        player,
                                        current.defaultNearRadius()
                                )
                        )
                )
                .then(Commands.argument(
                                        "radius",
                                        IntegerArgumentType.integer(
                                                1,
                                                current.maximumNearRadius()
                                        )
                                )
                                .executes(command ->
                                        PlayerStateCommandSupport.requirePlayer(
                                                context,
                                                command,
                                                descriptor,
                                                "near radius",
                                                players,
                                                player -> service.near(
                                                        player,
                                                        IntegerArgumentType.getInteger(
                                                                command,
                                                                "radius"
                                                        )
                                                )
                                        )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.near",
                "/near [radius]",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
