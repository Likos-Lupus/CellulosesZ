package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.BanCommandService;
import top.likoslupus.cellulosesz.modules.admin.command.argument.NetworkTargets;

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
                        StringArgumentType.greedyString()
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor
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

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) throws CommandSyntaxException {
        var target = NetworkTargets.addressOnly(
                StringArgumentType.getString(command, "address")
        );
        return AdminCommandResults.async(
                registration,
                command,
                descriptor,
                "unbanip",
                policy -> service.unbanIp(
                        target.address(),
                        AdminCommandResults.actor(policy, players)
                )
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
