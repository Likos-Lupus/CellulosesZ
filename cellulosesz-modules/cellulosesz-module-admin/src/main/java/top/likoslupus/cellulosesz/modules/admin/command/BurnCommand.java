package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.PlayerControlCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BurnCommand implements CommandContributor {

    private final PlayerControlCommandService service;
    private final int maximum;

    public BurnCommand(
            PlayerControlCommandService service,
            int maximum
    ) {
        this.service = requireNonNull(service, "service");
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
                        EntityArgument.player()
                )
                .then(Commands.argument(
                                        "seconds",
                                        IntegerArgumentType.integer(0, maximum)
                                )
                                .executes(command -> {
                                    var targetName = EntityArgument.getPlayer(command, "player")
                                            .getGameProfile()
                                            .name();

                                    return AdminCommandResults.async(
                                            context,
                                            command,
                                            descriptor,
                                            "burn seconds="
                                                    + IntegerArgumentType.getInteger(
                                                    command,
                                                    "seconds"
                                            ),
                                            _ -> service.burn(
                                                    targetName,
                                                    IntegerArgumentType.getInteger(
                                                            command,
                                                            "seconds"
                                                    )
                                            )
                                    );
                                })
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
