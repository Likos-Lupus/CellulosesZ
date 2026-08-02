package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;

import java.util.List;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class JumpCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;
    private final int maximumDistance;

    public JumpCommand(
            TeleportCommandService service,
            PlayerDirectory players,
            int maximumDistance
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.maximumDistance = requirePositive(maximumDistance, "maximumDistance");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "jump",
                "cellulosesz.teleport.jump",
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal("jump")
                .executes(command ->
                        TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "jump",
                                players,
                                player -> service.jump(player, maximumDistance)
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.jump",
                "/jump",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
