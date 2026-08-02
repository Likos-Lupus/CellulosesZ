package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TpHereCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;

    public TpHereCommand(
            TeleportCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        register(
                context,
                "tphere",
                "cellulosesz.teleport.tphere",
                false
        );
    }

    private void register(
            CommandRegistrationContext context,
            String name,
            String permission,
            boolean override
    ) {
        var descriptor = TeleportCommandResults.descriptor(
                name,
                permission,
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal(name)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(command -> {
                            var targetName = EntityArgument.getPlayer(command, "player")
                                    .getGameProfile()
                                    .name();

                            return TeleportCommandResults.player(
                                    context,
                                    command,
                                    descriptor,
                                    name,
                                    players,
                                    actor -> service.here(
                                            actor,
                                            targetName,
                                            override,
                                            context.hasPermission(
                                                    command.getSource(), permission + ".bypass"
                                            )
                                    )
                            );
                        })
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description." + name,
                "/" + name + " <player>",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
