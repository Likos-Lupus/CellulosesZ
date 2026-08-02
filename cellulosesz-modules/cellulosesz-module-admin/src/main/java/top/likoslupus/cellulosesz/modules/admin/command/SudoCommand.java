package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.PlayerControlCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class SudoCommand implements CommandContributor {

    private final PlayerControlCommandService service;
    private final PlayerDirectory players;

    public SudoCommand(
            PlayerControlCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "sudo",
                "cellulosesz.command.sudo",
                CommandSourceKind.ANY
        );

        var argument = Commands.argument(
                        "player",
                        EntityArgument.player()
                )
                .then(Commands.argument(
                                        "command",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> {
                                    var targetName = EntityArgument.getPlayer(command, "player")
                                            .getGameProfile()
                                            .name();

                                    return AdminCommandResults.async(
                                            context,
                                            command,
                                            descriptor,
                                            "sudo command-redacted length="
                                                    + StringArgumentType.getString(
                                                    command,
                                                    "command"
                                            ).length(),
                                            policy -> service.sudo(
                                                    AdminCommandResults.actor(
                                                            policy,
                                                            players
                                                    ),
                                                    targetName,
                                                    StringArgumentType.getString(
                                                            command,
                                                            "command"
                                                    )
                                            )
                                    );
                                })
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.sudo",
                "/sudo <player> <command>",
                Commands.literal("sudo").then(argument)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
