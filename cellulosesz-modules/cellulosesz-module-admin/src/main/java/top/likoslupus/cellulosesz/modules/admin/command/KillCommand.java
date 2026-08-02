package top.likoslupus.cellulosesz.modules.admin.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.PlayerControlCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class KillCommand implements CommandContributor {

    private final PlayerControlCommandService service;

    public KillCommand(PlayerControlCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "kill",
                "cellulosesz.command.kill",
                CommandSourceKind.ANY
        );

        var argument = Commands.argument(
                        "player",
                        EntityArgument.player()
                )
                .executes(command -> {
                    var targetName = EntityArgument.getPlayer(command, "player")
                            .getGameProfile()
                            .name();

                    return AdminCommandResults.async(
                            context,
                            command,
                            descriptor,
                            "kill force=" + context.hasPermission(
                                    command.getSource(),
                                    "cellulosesz.command.kill.force"
                            ),
                            _ -> service.kill(
                                    targetName,
                                    context.hasPermission(
                                            command.getSource(),
                                            "cellulosesz.command.kill.force"
                                    )
                            )
                    );
                });

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.kill",
                "/kill <player>",
                Commands.literal("kill").then(argument)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
