package top.likoslupus.cellulosesz.modules.admin.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.BanCommandService;
import top.likoslupus.cellulosesz.modules.admin.command.argument.NetworkTargetArgument;
import top.likoslupus.cellulosesz.modules.admin.command.argument.NetworkTargetInput;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class UnbanIpCommand implements CommandContributor {

    private final BanCommandService service;
    private final PlayerDirectory players;

    public UnbanIpCommand(
            BanCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "unbanip",
                "cellulosesz.admin.unbanip",
                CommandSourceKind.ANY
        );

        var argument = Commands.argument(
                        "address",
                        NetworkTargetArgument.addressOnly()
                )
                .executes(command -> AdminCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "unbanip",
                        policy -> service.unbanIp(
                                ((NetworkTargetInput.Address)
                                        NetworkTargetArgument.get(
                                                command,
                                                "address"
                                        )).address(),
                                AdminCommandResults.actor(
                                        policy,
                                        players
                                )
                        )
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.unbanip",
                "/unbanip <address>",
                Commands.literal("unbanip").then(argument)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
